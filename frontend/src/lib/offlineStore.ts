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

export type QueuedAnswer = {
  id?: number;
  questionId: number;
  mockExamId: number | null;
  selectedOption: number | null;
  shortAnswer: string | null;
  isCorrect: boolean | null;
  durationMs: number;
  createdAt: number;
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
  solveQueue: {
    key: number;
    value: QueuedAnswer;
    indexes: { byCreatedAt: number };
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
        if (!db.objectStoreNames.contains("solveQueue")) {
          const store = db.createObjectStore("solveQueue", {
            keyPath: "id",
            autoIncrement: true,
          });
          store.createIndex("byCreatedAt", "createdAt");
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

// ---- Solve queue (offline answer submissions) --------------------------

export async function enqueueAnswer(answer: QueuedAnswer): Promise<number> {
  const db = await getDb();
  const id = await db.add("solveQueue", answer);
  return id as number;
}

export async function listQueuedAnswers(): Promise<QueuedAnswer[]> {
  const db = await getDb();
  return db.getAllFromIndex("solveQueue", "byCreatedAt");
}

export async function removeQueuedAnswer(id: number): Promise<void> {
  const db = await getDb();
  await db.delete("solveQueue", id);
}

// ---- Test / dev helpers ------------------------------------------------

export async function clearAll(): Promise<void> {
  const db = await getDb();
  const tx = db.transaction(
    ["mockExams", "questions", "snapshotMeta", "solveQueue"],
    "readwrite",
  );
  await tx.objectStore("mockExams").clear();
  await tx.objectStore("questions").clear();
  await tx.objectStore("snapshotMeta").clear();
  await tx.objectStore("solveQueue").clear();
  await tx.done;
}
