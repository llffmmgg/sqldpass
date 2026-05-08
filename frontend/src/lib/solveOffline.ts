// Capacitor Android 앱 — 오프라인 모의고사 제출 큐.
//
// submitSolve() 가 네트워크 실패 + 앱 모드일 때 이 모듈로 fall back.
// MCQ 는 IndexedDB 의 correctOption 으로 로컬 채점, 단답/서술은 로컬 채점 불가 → 일단 false 로
// 표시한 뒤 온라인 복귀 시 서버 채점으로 덮어쓴다 (현재 결과 화면은 result state 로컬이라 자동 갱신).

import { getToken } from "@/lib/auth";
import {
  enqueuePendingSolve,
  getPendingSolveByLocalId,
  getQuestion,
  listPendingSolves,
  markPendingSolveSynced,
  type PendingSolve,
  type PendingSolveAnswer,
} from "@/lib/offlineStore";
import type {
  SolveAnswerRequest,
  SolveAnswerResponse,
  SolveRequest,
  SolveResponse,
} from "@/lib/api";

const SUBMIT_PATH = "/api/solves";

let drainInFlight: Promise<DrainResult> | null = null;

export type DrainResult = {
  succeeded: number;
  failed: number;
};

/**
 * 오프라인 제출 — 응답 화면을 즉시 띄울 수 있도록 합성 SolveResponse 를 반환하고
 * IndexedDB 큐에 저장한다. 합성 id 는 항상 음수 (-Date.now()). 결과 페이지/상세 페이지는
 * id < 0 을 보고 "오프라인 결과" 분기를 탄다.
 */
export async function submitSolveOffline(request: SolveRequest): Promise<SolveResponse> {
  if (request.mockExamId == null) {
    // 일반 /solve (랜덤 풀이) 흐름은 일단 오프라인 미지원 — 회차 단위만 큐잉.
    throw new Error("오프라인에선 모의고사만 제출할 수 있어요.");
  }

  const localId = -Date.now();
  const graded = await gradeAnswers(request.answers);
  const localCorrectCount = graded.filter((g) => g.localCorrect === true).length;
  const totalCount = graded.length;

  const pending: PendingSolve = {
    localId,
    mockExamId: request.mockExamId,
    totalCount,
    localCorrectCount,
    answers: graded,
    createdAt: Date.now(),
    synced: false,
    serverSolveId: null,
    syncedAt: null,
  };
  await enqueuePendingSolve(pending);

  // 결과 화면에 줄 합성 응답. SolveAnswerResponse 의 correctOption 은 number 라 MCQ 만 채워지고
  // 비MCQ 는 0 으로 채워둔다 (UI 에서 questionType 보고 알아서 처리).
  const answerResponses: SolveAnswerResponse[] = graded.map((g) => ({
    questionId: g.questionId,
    selectedOption: g.selectedOption ?? 0,
    correctOption: g.correctOption ?? 0,
    correct: g.localCorrect === true,
  }));

  const score =
    totalCount === 0 ? 0 : Math.round((localCorrectCount / totalCount) * 100);

  return {
    id: localId,
    subjectId: null,
    mockExamId: request.mockExamId,
    totalCount,
    correctCount: localCorrectCount,
    score,
    solvedAt: new Date(pending.createdAt).toISOString(),
    answers: answerResponses,
    currentStreak: null,
    milestoneReached: null,
  };
}

async function gradeAnswers(reqAnswers: SolveAnswerRequest[]): Promise<PendingSolveAnswer[]> {
  const out: PendingSolveAnswer[] = [];
  for (const a of reqAnswers) {
    const q = await getQuestion(a.questionId).catch(() => null);
    let localCorrect: boolean | null = null;
    if (q && q.questionType === "MCQ" && q.correctOption != null) {
      localCorrect = a.selectedOption != null && a.selectedOption === q.correctOption;
    }
    out.push({
      questionId: a.questionId,
      selectedOption: a.selectedOption,
      answerText: a.answerText,
      localCorrect,
      correctOption: q?.correctOption ?? null,
    });
  }
  return out;
}

/**
 * 큐에 쌓여 있던 미동기 회차 제출을 서버에 일괄 전송.
 * 동시 호출 방지를 위해 단일 promise 로 직렬화. 실패한 제출은 다음 drain 시 재시도.
 */
export function drainPendingSolves(): Promise<DrainResult> {
  if (drainInFlight) return drainInFlight;
  drainInFlight = (async () => {
    let succeeded = 0;
    let failed = 0;
    try {
      const pending = await listPendingSolves({ onlyUnsynced: true });
      for (const row of pending) {
        if (row.id == null) continue;
        try {
          const serverSolveId = await postSolve(row);
          await markPendingSolveSynced(row.id, serverSolveId);
          succeeded += 1;
        } catch {
          failed += 1;
        }
      }
    } finally {
      drainInFlight = null;
    }
    return { succeeded, failed };
  })();
  return drainInFlight;
}

async function postSolve(row: PendingSolve): Promise<number> {
  const token = getToken();
  if (!token) {
    // 인증 토큰 만료/로그아웃 — 한 번 더 시도해도 똑같이 실패할 거라 throw 한다.
    throw new Error("로그인이 필요합니다.");
  }
  const payload = {
    mockExamId: row.mockExamId,
    answers: row.answers.map((a) => {
      const item: SolveAnswerRequest = { questionId: a.questionId };
      if (a.selectedOption != null) item.selectedOption = a.selectedOption;
      if (a.answerText != null) item.answerText = a.answerText;
      return item;
    }),
  };
  const res = await fetch(SUBMIT_PATH, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    throw new Error(`solve submit failed (${res.status})`);
  }
  const body = (await res.json()) as SolveResponse;
  return body.id;
}

/**
 * 합성 음수 id 로 결과 페이지에 들어왔을 때, 큐에 보관된 결과를 SolveResponse 형태로 복원.
 * 동기화 후엔 serverSolveId 로 라우팅을 갈아끼우는 게 깔끔하지만, 일단 로컬 view 만 지원.
 */
export async function getOfflineSolveByLocalId(localId: number): Promise<SolveResponse | null> {
  const row = await getPendingSolveByLocalId(localId);
  if (!row) return null;
  return {
    id: row.localId,
    subjectId: null,
    mockExamId: row.mockExamId,
    totalCount: row.totalCount,
    correctCount: row.localCorrectCount,
    score:
      row.totalCount === 0
        ? 0
        : Math.round((row.localCorrectCount / row.totalCount) * 100),
    solvedAt: new Date(row.createdAt).toISOString(),
    answers: row.answers.map((a) => ({
      questionId: a.questionId,
      selectedOption: a.selectedOption ?? 0,
      correctOption: a.correctOption ?? 0,
      correct: a.localCorrect === true,
    })),
    currentStreak: null,
    milestoneReached: null,
  };
}
