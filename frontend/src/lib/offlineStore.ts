// IndexedDB wrapper for the Capacitor Android app's offline-first content cache.
// Web users do not hit this code — see isCapacitorApp() guards in callers.
//
// Schema is intentionally denormalised: questions are duplicated under their
// mockExam so the solve screen can load a whole 회차 with one transaction.

import { openDB, type DBSchema, type IDBPDatabase } from "idb";

export type OfflineMockExam = {
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
  questionIds: number[];
};

export type OfflineQuestion = {
  id: number;
  mockExamId: number;
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
};

export type SnapshotMeta = {
  key: "version" | "lastSyncedAt" | "etag";
  value: string;
};

/**
 * 오프라인 모의고사 제출 큐 — 한 row 가 한 번의 회차 제출 시도(=서버 /api/solves POST 한 번)에 대응.
 * synced 가 true 면 이미 서버에 동기화 완료 + serverSolveId 가 채워진 상태 (히스토리 deeplink 용으로 일정 기간 보존).
 */
export type PendingSolveAnswer = {
  questionId: number;
  selectedOption?: number;
  answerText?: string;
  /** 로컬 MCQ 채점 결과. SHORT/DESC 등 로컬 채점 불가 항목은 null. */
  localCorrect: boolean | null;
  /** 회차 정답 (MCQ 만 채워짐). 결과 화면이 SolveAnswerResponse 형태로 렌더하기 위함. */
  correctOption: number | null;
};

export type PendingSolve = {
  id?: number;
  /** 합성 SolveResponse 의 음수 id — submitSolveOffline 호출 시점에 -Date.now() 부여. */
  localId: number;
  mockExamId: number;
  totalCount: number;
  /** 로컬 채점된 정답 수 (MCQ 만, 미채점 항목은 isCorrect=null 이므로 미포함). */
  localCorrectCount: number;
  answers: PendingSolveAnswer[];
  createdAt: number;
  synced: boolean;
  serverSolveId: number | null;
  syncedAt: number | null;
};

interface SqldpassOfflineDB extends DBSchema {
  mockExams: {
    key: number;
    value: OfflineMockExam;
    indexes: { byExamType: string };
  };
  questions: {
    key: number;
    value: OfflineQuestion;
    indexes: { byMockExam: number };
  };
  snapshotMeta: {
    key: SnapshotMeta["key"];
    value: SnapshotMeta;
  };
  pendingSolves: {
    key: number;
    value: PendingSolve;
    indexes: { byCreatedAt: number; bySynced: number; byLocalId: number };
  };
}

const DB_NAME = "sqldpass-offline";
const DB_VERSION = 1;

let dbPromise: Promise<IDBPDatabase<SqldpassOfflineDB>> | null = null;

function getDb() {
  if (typeof window === "undefined" || typeof indexedDB === "undefined") {
    return Promise.reject(new Error("IndexedDB is unavailable in this environment"));
  }
  if (!dbPromise) {
    dbPromise = openDB<SqldpassOfflineDB>(DB_NAME, DB_VERSION, {
      upgrade(db) {
        if (!db.objectStoreNames.contains("mockExams")) {
          const store = db.createObjectStore("mockExams", { keyPath: "id" });
          store.createIndex("byExamType", "examType");
        }
        if (!db.objectStoreNames.contains("questions")) {
          const store = db.createObjectStore("questions", { keyPath: "id" });
          store.createIndex("byMockExam", "mockExamId");
        }
        if (!db.objectStoreNames.contains("snapshotMeta")) {
          db.createObjectStore("snapshotMeta", { keyPath: "key" });
        }
        if (!db.objectStoreNames.contains("pendingSolves")) {
          const store = db.createObjectStore("pendingSolves", {
            keyPath: "id",
            autoIncrement: true,
          });
          store.createIndex("byCreatedAt", "createdAt");
          // synced 는 boolean 인데 IndexedDB 는 boolean 인덱스를 지원 안 해 0/1 정수로 저장한다.
          store.createIndex("bySynced", "syncedFlag");
          store.createIndex("byLocalId", "localId", { unique: true });
        }
      },
    });
  }
  return dbPromise;
}

// ---- Snapshot meta ------------------------------------------------------

export async function getMeta(key: SnapshotMeta["key"]): Promise<string | null> {
  const db = await getDb();
  const row = await db.get("snapshotMeta", key);
  return row?.value ?? null;
}

export async function setMeta(key: SnapshotMeta["key"], value: string): Promise<void> {
  const db = await getDb();
  await db.put("snapshotMeta", { key, value });
}

// ---- Snapshot replace ---------------------------------------------------

/**
 * Replace the cached content with a fresh snapshot. Single transaction so the
 * cache is never observed in a half-written state.
 */
export async function replaceSnapshot(
  mockExams: OfflineMockExam[],
  questions: OfflineQuestion[],
  meta: { version: string; etag: string },
): Promise<void> {
  const db = await getDb();
  const tx = db.transaction(["mockExams", "questions", "snapshotMeta"], "readwrite");
  await tx.objectStore("mockExams").clear();
  await tx.objectStore("questions").clear();
  for (const exam of mockExams) await tx.objectStore("mockExams").put(exam);
  for (const q of questions) await tx.objectStore("questions").put(q);
  await tx.objectStore("snapshotMeta").put({ key: "version", value: meta.version });
  await tx.objectStore("snapshotMeta").put({ key: "etag", value: meta.etag });
  await tx
    .objectStore("snapshotMeta")
    .put({ key: "lastSyncedAt", value: new Date().toISOString() });
  await tx.done;
}

// ---- Reads --------------------------------------------------------------

export async function listMockExams(): Promise<OfflineMockExam[]> {
  const db = await getDb();
  return db.getAll("mockExams");
}

export async function getMockExam(id: number): Promise<OfflineMockExam | null> {
  const db = await getDb();
  const row = await db.get("mockExams", id);
  return row ?? null;
}

export async function getQuestionsForMockExam(mockExamId: number): Promise<OfflineQuestion[]> {
  const db = await getDb();
  const rows = await db.getAllFromIndex("questions", "byMockExam", mockExamId);
  rows.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  return rows;
}

export async function getQuestion(id: number): Promise<OfflineQuestion | null> {
  const db = await getDb();
  const row = await db.get("questions", id);
  return row ?? null;
}

export async function getCacheStats(): Promise<{ mockExams: number; questions: number }> {
  const db = await getDb();
  const [mockExams, questions] = await Promise.all([
    db.count("mockExams"),
    db.count("questions"),
  ]);
  return { mockExams, questions };
}

// ---- Pending solves (offline mock-exam submissions) -------------------

type PendingSolveRow = PendingSolve & { syncedFlag: 0 | 1 };

function withSyncedFlag(p: PendingSolve): PendingSolveRow {
  return { ...p, syncedFlag: p.synced ? 1 : 0 };
}

function stripSyncedFlag(row: PendingSolveRow): PendingSolve {
  const { syncedFlag: _ignored, ...rest } = row;
  return rest;
}

export async function enqueuePendingSolve(solve: PendingSolve): Promise<number> {
  const db = await getDb();
  const id = (await db.add("pendingSolves", withSyncedFlag(solve) as PendingSolve)) as number;
  return id;
}

export async function listPendingSolves(opts?: { onlyUnsynced?: boolean }): Promise<PendingSolve[]> {
  const db = await getDb();
  const rows = (await db.getAllFromIndex("pendingSolves", "byCreatedAt")) as PendingSolveRow[];
  const filtered = opts?.onlyUnsynced ? rows.filter((r) => !r.synced) : rows;
  return filtered.map(stripSyncedFlag);
}

export async function getPendingSolveByLocalId(localId: number): Promise<PendingSolve | null> {
  const db = await getDb();
  const row = (await db.getFromIndex("pendingSolves", "byLocalId", localId)) as
    | PendingSolveRow
    | undefined;
  return row ? stripSyncedFlag(row) : null;
}

export async function markPendingSolveSynced(id: number, serverSolveId: number): Promise<void> {
  const db = await getDb();
  const tx = db.transaction("pendingSolves", "readwrite");
  const row = (await tx.store.get(id)) as PendingSolveRow | undefined;
  if (row) {
    const updated: PendingSolveRow = {
      ...row,
      synced: true,
      syncedFlag: 1,
      serverSolveId,
      syncedAt: Date.now(),
    };
    await tx.store.put(updated);
  }
  await tx.done;
}

export async function removePendingSolve(id: number): Promise<void> {
  const db = await getDb();
  await db.delete("pendingSolves", id);
}

// ---- Test / dev helpers ------------------------------------------------

export async function clearAll(): Promise<void> {
  const db = await getDb();
  const tx = db.transaction(
    ["mockExams", "questions", "snapshotMeta", "pendingSolves"],
    "readwrite",
  );
  await tx.objectStore("mockExams").clear();
  await tx.objectStore("questions").clear();
  await tx.objectStore("snapshotMeta").clear();
  await tx.objectStore("pendingSolves").clear();
  await tx.done;
}
