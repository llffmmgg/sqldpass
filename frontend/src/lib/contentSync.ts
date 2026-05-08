// First-boot snapshot prefetch for the Capacitor Android app.
// Fetches /api/content/snapshot (ETag-aware) and replaces the IndexedDB cache
// in a single transaction so the app can fall back to offline solving for any
// 회차 — not just ones the user has visited.

import {
  getMeta,
  setMeta,
  replaceSnapshot,
  type OfflineMockExam,
  type OfflineQuestion,
} from "@/lib/offlineStore";

const SNAPSHOT_PATH = "/api/content/snapshot";

type SnapshotPayload = {
  version: string;
  generatedAt: string;
  mockExamCount: number;
  questionCount: number;
  mockExams: Array<{
    id: number;
    name: string;
    examType: string;
    sequence: number;
    visibility: string;
    expertVerified: boolean;
    kind: string;
    examYear: number | null;
    examRound: number | null;
    examDate: string | null;
    template: string | null;
    questions: Array<{
      id: number;
      displayOrder: number | null;
      subjectId: number | null;
      subjectName: string | null;
      subjectParentName: string | null;
      content: string;
      questionType: string;
      correctOption: number | null;
      answer: string | null;
      keywords: string[];
      explanation: string | null;
      summary: string | null;
      topic: string | null;
      difficulty: number | null;
    }>;
  }>;
};

export type SyncProgress = {
  phase: "checking" | "downloading" | "saving" | "done" | "skipped" | "error";
  /** Bytes received so far (downloading phase). */
  receivedBytes?: number;
  /** Total bytes hinted by Content-Length, if known. */
  totalBytes?: number;
  /** Last known cache stats — set on done/skipped. */
  cachedMockExams?: number;
  cachedQuestions?: number;
  error?: string;
};

export type SyncOptions = {
  onProgress?: (progress: SyncProgress) => void;
  /** Skip the network entirely if a non-empty cache exists. Default: false. */
  preferCacheIfPresent?: boolean;
};

export class ContentSyncError extends Error {
  constructor(message: string, readonly cause?: unknown) {
    super(message);
    this.name = "ContentSyncError";
  }
}

/**
 * Fetch and apply the latest content snapshot.
 * Returns true if the cache was updated, false if the server replied 304 or
 * sync was skipped because the cache was already populated.
 */
export async function syncContent(opts: SyncOptions = {}): Promise<boolean> {
  const onProgress = opts.onProgress ?? noop;

  try {
    if (opts.preferCacheIfPresent) {
      const existing = await getMeta("version");
      if (existing) {
        onProgress({ phase: "skipped" });
        return false;
      }
    }

    onProgress({ phase: "checking" });

    const cachedEtag = await getMeta("etag");
    const headers: Record<string, string> = { Accept: "application/json" };
    if (cachedEtag) headers["If-None-Match"] = cachedEtag;

    const res = await fetch(SNAPSHOT_PATH, { headers, cache: "no-store" });

    if (res.status === 304) {
      // Server says our cache is still current. Touch lastSyncedAt and bail.
      await setMeta("lastSyncedAt", new Date().toISOString());
      onProgress({ phase: "skipped" });
      return false;
    }

    if (!res.ok) {
      throw new ContentSyncError(`스냅샷 다운로드 실패 (${res.status})`);
    }

    const totalBytes = parseContentLength(res.headers.get("Content-Length"));
    onProgress({ phase: "downloading", receivedBytes: 0, totalBytes });

    const text = await readBodyWithProgress(res, totalBytes, (received) => {
      onProgress({ phase: "downloading", receivedBytes: received, totalBytes });
    });

    onProgress({ phase: "saving" });

    let payload: SnapshotPayload;
    try {
      payload = JSON.parse(text) as SnapshotPayload;
    } catch (e) {
      throw new ContentSyncError("스냅샷 파싱 실패", e);
    }

    const { mockExams, questions } = flatten(payload);
    const etag = res.headers.get("ETag") ?? `"${payload.version}"`;
    await replaceSnapshot(mockExams, questions, { version: payload.version, etag });

    onProgress({
      phase: "done",
      cachedMockExams: mockExams.length,
      cachedQuestions: questions.length,
    });
    return true;
  } catch (err) {
    const message = err instanceof Error ? err.message : "콘텐츠 동기화에 실패했습니다.";
    onProgress({ phase: "error", error: message });
    if (err instanceof ContentSyncError) throw err;
    throw new ContentSyncError(message, err);
  }
}

function flatten(payload: SnapshotPayload): {
  mockExams: OfflineMockExam[];
  questions: OfflineQuestion[];
} {
  const mockExams: OfflineMockExam[] = [];
  const questions: OfflineQuestion[] = [];
  for (const exam of payload.mockExams) {
    const questionIds = exam.questions.map((q) => q.id);
    mockExams.push({
      id: exam.id,
      name: exam.name,
      examType: exam.examType,
      sequence: exam.sequence,
      visibility: exam.visibility,
      expertVerified: exam.expertVerified,
      kind: exam.kind,
      examYear: exam.examYear,
      examRound: exam.examRound,
      examDate: exam.examDate,
      template: exam.template,
      questionIds,
    });
    for (const q of exam.questions) {
      questions.push({
        id: q.id,
        mockExamId: exam.id,
        displayOrder: q.displayOrder,
        subjectId: q.subjectId,
        subjectName: q.subjectName,
        subjectParentName: q.subjectParentName,
        content: q.content,
        questionType: q.questionType,
        correctOption: q.correctOption,
        answer: q.answer,
        keywords: q.keywords,
        explanation: q.explanation,
        summary: q.summary,
        topic: q.topic,
        difficulty: q.difficulty,
      });
    }
  }
  return { mockExams, questions };
}

async function readBodyWithProgress(
  res: Response,
  totalBytes: number | undefined,
  onChunk: (received: number) => void,
): Promise<string> {
  // Without a streaming body we can't report progress — fall back to text().
  if (!res.body) {
    return res.text();
  }
  const reader = res.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let received = 0;
  let result = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    if (value) {
      received += value.byteLength;
      result += decoder.decode(value, { stream: true });
      onChunk(received);
    }
  }
  result += decoder.decode();
  void totalBytes; // currently unused beyond the initial onProgress hint
  return result;
}

function parseContentLength(value: string | null): number | undefined {
  if (!value) return undefined;
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? n : undefined;
}

function noop() {}
