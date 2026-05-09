"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { use } from "react";
import {
  downloadMockExamPdf,
  getAdminMockExamDetail,
  getAdminMockExams,
  getQuestion,
  markMockExamVerified,
  setPastExamMeta,
  toggleExpertVerified,
  updateQuestion,
  type AdminQuestionType,
  type AdminMockExam,
  type AdminMockExamDetail,
  type AdminQuestion,
} from "@/lib/adminApi";

type WebReviewQuestion = {
  id: number;
  order: number;
  subjectId: number;
  subjectName: string;
  questionType: AdminQuestionType;
  content: string;
  correctOption: number | null;
  answer: string | null;
  keywords: string[] | null;
  explanation: string;
  summary: string | null;
};

type WebReviewValidation = {
  ok: boolean;
  errors: string[];
  changedCount: number;
  changes: string[];
  payload: WebReviewQuestion[];
};

type WebReviewAutomationPackage = {
  kind: "sqldpass.webLlmReview";
  version: 1;
  status: "ready" | "blocked";
  errors: string[];
  adminUrl: string;
  claudeProjectUrl: string;
  chatgptProjectUrl: string;
  originalJson: string;
  target: {
    automationPackageTestId: "web-review-automation-package";
    runCommandTestId: "web-review-codex-run-command";
    finalJsonTestId: "web-review-final-json";
    validateButtonTestId: "web-review-validate-final";
  };
  flow: string[];
};

const WEB_REVIEW_CLAUDE_URL_KEY = "sqldpass.webReview.claudeProjectUrl";
const WEB_REVIEW_CHATGPT_URL_KEY = "sqldpass.webReview.chatgptProjectUrl";

function readStoredValue(key: string): string {
  if (typeof window === "undefined") return "";
  return window.localStorage.getItem(key) ?? "";
}

/**
 * 어드민: 모의고사 상세 — 문제별 인라인 수정.
 *
 * 흐름:
 *  1) /api/mock-exams/{id} 로 회차 + 문제 id 목록 + displayOrder 가져옴
 *  2) 각 문제는 /api/admin/questions/{id} 로 상세(정답/해설 포함) 조회
 *  3) 각 카드는 textarea 4개(content/correctOption/explanation/summary)로 인라인 편집
 *  4) "저장" 클릭 시 PUT /api/admin/questions/{id} 호출
 */
export default function AdminMockExamDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const examId = Number(id);

  const [exam, setExam] = useState<AdminMockExamDetail | null>(null);
  const [questions, setQuestions] = useState<AdminQuestion[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [verifyResult, setVerifyResult] = useState<string | null>(null);
  const [pdfBusy, setPdfBusy] = useState(false);
  const [pdfStatus, setPdfStatus] = useState<string | null>(null);
  const [allExams, setAllExams] = useState<AdminMockExam[] | null>(null);
  const [webReviewOpen, setWebReviewOpen] = useState(false);
  const [webReviewOriginalJson, setWebReviewOriginalJson] = useState("");
  const [webReviewClaudeJson, setWebReviewClaudeJson] = useState("");
  const [webReviewFinalJson, setWebReviewFinalJson] = useState("");
  const [webReviewResult, setWebReviewResult] = useState<string | null>(null);
  const [webReviewSaving, setWebReviewSaving] = useState(false);
  const [webReviewClaudeProjectUrl, setWebReviewClaudeProjectUrl] = useState(() =>
    readStoredValue(WEB_REVIEW_CLAUDE_URL_KEY),
  );
  const [webReviewChatgptProjectUrl, setWebReviewChatgptProjectUrl] = useState(() =>
    readStoredValue(WEB_REVIEW_CHATGPT_URL_KEY),
  );
  const [jsonMode, setJsonMode] = useState(false);
  const [jsonText, setJsonText] = useState("");
  const [jsonSaving, setJsonSaving] = useState(false);
  const [jsonResult, setJsonResult] = useState<string | null>(null);

  // 같은 시험유형 내 이전/다음 모의고사
  const { prevExam, nextExam } = useMemo(() => {
    if (!allExams || !exam) return { prevExam: null, nextExam: null };
    const sameType = allExams
      .filter((e) => e.examType === exam.examType)
      .sort((a, b) => a.sequence - b.sequence);
    const idx = sameType.findIndex((e) => e.id === examId);
    return {
      prevExam: idx > 0 ? sameType[idx - 1] : null,
      nextExam: idx < sameType.length - 1 ? sameType[idx + 1] : null,
    };
  }, [allExams, exam, examId]);

  useEffect(() => {
    getAdminMockExams().then(setAllExams).catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    setJsonMode(false);
    setJsonResult(null);
    setWebReviewOpen(false);
    setWebReviewOriginalJson("");
    setWebReviewClaudeJson("");
    setWebReviewFinalJson("");
    setWebReviewResult(null);
    (async () => {
      try {
        const detail = await getAdminMockExamDetail(examId);
        if (cancelled) return;
        setExam(detail);
        // 문제별 admin 상세 병렬 조회
        const fulls = await Promise.all(
          detail.questions
            .slice()
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((q) => getQuestion(q.id)),
        );
        if (cancelled) return;
        setQuestions(fulls);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : "불러오기 실패");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [examId]);

  async function handleMarkVerified() {
    if (!exam || !questions) return;
    const unverified = questions.filter((q) => !q.verifiedAt).length;
    if (unverified === 0) {
      setVerifyResult("모든 문제가 이미 검수 완료되었습니다.");
      return;
    }
    if (!confirm(`${unverified}문항을 수동으로 검수 완료 처리합니다. 계속하시겠습니까?`)) return;

    try {
      const result = await markMockExamVerified(examId);
      const fulls = await Promise.all(
        exam.questions
          .slice()
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .map((q) => getQuestion(q.id)),
      );
      setQuestions(fulls);
      setVerifyResult(`${result.marked}문항 수동 검수 완료 처리됨`);
    } catch (e) {
      setVerifyResult(`처리 실패: ${e instanceof Error ? e.message : "알 수 없는 오류"}`);
    }
  }

  function buildWebReviewQuestions(source: AdminQuestion[]): WebReviewQuestion[] {
    return source.map((q, i) => ({
      id: q.id,
      order: i + 1,
      subjectId: q.subjectId,
      subjectName: q.subjectName,
      questionType: q.questionType,
      content: q.content,
      correctOption: q.correctOption,
      answer: q.answer,
      keywords: q.keywords,
      explanation: q.explanation,
      summary: q.summary,
    }));
  }

  function openWebReview() {
    if (!questions) return;
    setWebReviewOriginalJson(JSON.stringify(buildWebReviewQuestions(questions), null, 2));
    setWebReviewClaudeJson("");
    setWebReviewFinalJson("");
    setWebReviewResult("원본 JSON을 Claude 프로젝트에 그대로 전달하세요.");
    setWebReviewOpen(true);
  }

  async function copyWebReviewText(value: string, label: string) {
    if (!value.trim()) {
      setWebReviewResult(`${label}이 비어 있습니다.`);
      return;
    }
    try {
      await navigator.clipboard.writeText(value);
      setWebReviewResult(`${label}을 클립보드에 복사했습니다.`);
    } catch (e) {
      setWebReviewResult(`복사 실패: ${e instanceof Error ? e.message : String(e)}`);
    }
  }

  function saveWebReviewProjectUrls() {
    const claudeUrl = webReviewClaudeProjectUrl.trim();
    const chatgptUrl = webReviewChatgptProjectUrl.trim();

    if (!claudeUrl || !chatgptUrl) {
      setWebReviewResult("Claude/ChatGPT 프로젝트 URL을 모두 입력해야 합니다.");
      return;
    }

    window.localStorage.setItem(WEB_REVIEW_CLAUDE_URL_KEY, claudeUrl);
    window.localStorage.setItem(WEB_REVIEW_CHATGPT_URL_KEY, chatgptUrl);
    setWebReviewClaudeProjectUrl(claudeUrl);
    setWebReviewChatgptProjectUrl(chatgptUrl);
    setWebReviewResult("프로젝트 URL을 저장했습니다. 이후 다른 모의고사에서도 재사용됩니다.");
  }

  function buildWebReviewAutomationPackage(): WebReviewAutomationPackage {
    const claudeProjectUrl = webReviewClaudeProjectUrl.trim();
    const chatgptProjectUrl = webReviewChatgptProjectUrl.trim();
    const originalJson = webReviewOriginalJson.trim();
    const errors: string[] = [];

    if (!claudeProjectUrl || !chatgptProjectUrl) {
      errors.push("Claude/ChatGPT 프로젝트 URL을 모두 저장해야 합니다.");
    }
    if (!originalJson) {
      errors.push("원본 JSON이 비어 있습니다. 웹 LLM 검수 패널을 다시 열어야 합니다.");
    }

    return {
      kind: "sqldpass.webLlmReview",
      version: 1,
      status: errors.length > 0 ? "blocked" : "ready",
      errors,
      adminUrl: typeof window === "undefined" ? "" : window.location.href,
      claudeProjectUrl,
      chatgptProjectUrl,
      originalJson,
      target: {
        automationPackageTestId: "web-review-automation-package",
        runCommandTestId: "web-review-codex-run-command",
        finalJsonTestId: "web-review-final-json",
        validateButtonTestId: "web-review-validate-final",
      },
      flow: [
        "open claudeProjectUrl",
        "send originalJson only",
        "wait for Claude full JSON response",
        "open chatgptProjectUrl",
        "send Claude response JSON only",
        "wait for ChatGPT final JSON response",
        "return to adminUrl",
        "fill web-review-final-json",
        "click web-review-validate-final",
      ],
    };
  }

  function buildWebReviewCodexRunCommand(pkg: WebReviewAutomationPackage): string {
    if (pkg.status === "blocked") {
      return `자동 실행 전 해결 필요:\n${pkg.errors.map((item) => `- ${item}`).join("\n")}`;
    }

    return [
      "Codex 인앱 브라우저로 이 자동 실행 패키지를 읽고 웹 LLM 검수를 수행해줘.",
      "1. claudeProjectUrl을 열고 originalJson만 그대로 전송해.",
      "2. Claude가 반환한 전체 JSON 응답을 추출해.",
      "3. chatgptProjectUrl을 열고 Claude 응답 JSON만 그대로 전송해.",
      "4. ChatGPT가 반환한 최종 전체 JSON 응답을 추출해.",
      "5. adminUrl로 돌아와 data-testid=\"web-review-final-json\" 입력칸에 최종 JSON을 채워.",
      "6. data-testid=\"web-review-validate-final\" 버튼을 눌러 검증해.",
      "7. data-testid=\"web-review-apply-final\" 버튼은 누르지 마. 운영자가 diff와 검증 결과를 확인한 뒤 직접 적용한다.",
    ].join("\n");
  }

  async function copyWebReviewAutomationPackage() {
    const pkg = buildWebReviewAutomationPackage();
    if (pkg.status === "blocked") {
      setWebReviewResult(`자동 실행 패키지 생성 차단\n${pkg.errors.join("\n")}`);
      return;
    }
    await copyWebReviewText(JSON.stringify(pkg, null, 2), "Codex 자동 실행 패키지");
  }

  function parseJsonArrayText(text: string): unknown {
    const trimmed = text.trim();
    if (!trimmed) throw new Error("JSON이 비어 있습니다.");
    try {
      return JSON.parse(trimmed);
    } catch {
      const fenced = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i);
      if (fenced?.[1]) return JSON.parse(fenced[1].trim());

      const start = trimmed.indexOf("[");
      const end = trimmed.lastIndexOf("]");
      if (start >= 0 && end > start) {
        return JSON.parse(trimmed.slice(start, end + 1));
      }
      throw new Error("JSON 배열을 찾을 수 없습니다.");
    }
  }

  function validateWebReviewJson(text: string): WebReviewValidation {
    const errors: string[] = [];
    const changes: string[] = [];
    const payload: WebReviewQuestion[] = [];
    const original = buildWebReviewQuestions(questions ?? []);
    const originalById = new Map(original.map((q) => [q.id, q]));

    let parsed: unknown;
    try {
      parsed = parseJsonArrayText(text);
    } catch (e) {
      return {
        ok: false,
        errors: [e instanceof Error ? e.message : "JSON 파싱에 실패했습니다."],
        changedCount: 0,
        changes: [],
        payload: [],
      };
    }

    if (!Array.isArray(parsed)) {
      return {
        ok: false,
        errors: ["최종 JSON은 배열이어야 합니다."],
        changedCount: 0,
        changes: [],
        payload: [],
      };
    }

    if (parsed.length !== original.length) {
      errors.push(`문항 수가 다릅니다. 원본 ${original.length}개, 최종 ${parsed.length}개`);
    }

    const seenIds = new Set<number>();
    parsed.forEach((raw, index) => {
      const item = raw as Partial<WebReviewQuestion>;
      const id = Number(item.id);
      const originalItem = originalById.get(id);
      const label = Number.isFinite(id) ? `ID ${id}` : `${index + 1}번째 문항`;

      if (!Number.isFinite(id)) {
        errors.push(`${label}: id가 유효하지 않습니다.`);
        return;
      }
      if (seenIds.has(id)) {
        errors.push(`${label}: id가 중복되었습니다.`);
        return;
      }
      seenIds.add(id);
      if (!originalItem) {
        errors.push(`${label}: 원본에 없는 id입니다.`);
        return;
      }

      if (item.order !== originalItem.order) {
        errors.push(`${label}: order를 변경할 수 없습니다.`);
      }
      if (item.subjectId !== originalItem.subjectId) {
        errors.push(`${label}: subjectId를 변경할 수 없습니다.`);
      }
      if (item.questionType !== originalItem.questionType) {
        errors.push(`${label}: questionType을 변경할 수 없습니다.`);
      }

      const content = typeof item.content === "string" ? item.content : "";
      const explanation = typeof item.explanation === "string" ? item.explanation : "";
      const summary = typeof item.summary === "string" && item.summary.trim() ? item.summary : null;
      const answer = typeof item.answer === "string" ? item.answer : null;
      const hasKeywords = Object.prototype.hasOwnProperty.call(item, "keywords");
      const rawKeywords = (item as { keywords?: unknown }).keywords;
      let keywords = originalItem.keywords;

      if (!content.trim()) {
        errors.push(`${label}: content가 비어 있습니다.`);
      }
      if (!explanation.trim()) {
        errors.push(`${label}: explanation이 비어 있습니다.`);
      }

      const questionType = originalItem.questionType;
      let correctOption: number | null = null;
      if (questionType === "MCQ") {
        correctOption = Number(item.correctOption);
        if (!Number.isInteger(correctOption) || correctOption < 1 || correctOption > 4) {
          errors.push(`${label}: MCQ correctOption은 1~4여야 합니다.`);
        }
      } else if (!answer?.trim()) {
        errors.push(`${label}: ${questionType} answer가 비어 있습니다.`);
      } else if (hasKeywords && rawKeywords != null) {
        if (!Array.isArray(rawKeywords)) {
          errors.push(`${label}: ${questionType} keywords는 문자열 배열이어야 합니다.`);
        } else if (rawKeywords.some((keyword) => typeof keyword !== "string")) {
          errors.push(`${label}: ${questionType} keywords에는 문자열만 사용할 수 있습니다.`);
        } else {
          keywords = rawKeywords;
        }
      }

      const normalized: WebReviewQuestion = {
        id,
        order: originalItem.order,
        subjectId: originalItem.subjectId,
        subjectName: originalItem.subjectName,
        questionType,
        content,
        correctOption: questionType === "MCQ" ? correctOption : null,
        answer: questionType === "MCQ" ? null : answer,
        keywords: questionType === "MCQ" ? null : keywords,
        explanation,
        summary,
      };
      payload.push(normalized);

      const changedFields = [
        normalized.content !== originalItem.content ? "content" : null,
        normalized.correctOption !== originalItem.correctOption ? "correctOption" : null,
        normalized.answer !== originalItem.answer ? "answer" : null,
        JSON.stringify(normalized.keywords ?? null) !== JSON.stringify(originalItem.keywords ?? null) ? "keywords" : null,
        normalized.explanation !== originalItem.explanation ? "explanation" : null,
        normalized.summary !== originalItem.summary ? "summary" : null,
      ].filter(Boolean);

      if (changedFields.length > 0) {
        changes.push(`#${originalItem.order} ${label}: ${changedFields.join(", ")}`);
      }
    });

    return {
      ok: errors.length === 0,
      errors,
      changedCount: changes.length,
      changes,
      payload,
    };
  }

  function handleValidateWebReviewFinal() {
    const result = validateWebReviewJson(webReviewFinalJson);
    if (!result.ok) {
      setWebReviewResult(`검증 실패\n${result.errors.join("\n")}`);
      return;
    }
    const changePreview = result.changes.slice(0, 30).join("\n");
    setWebReviewResult(
      `검증 통과: ${result.payload.length}문항, 변경 ${result.changedCount}문항` +
        (changePreview ? `\n\n${changePreview}` : ""),
    );
  }

  async function handleApplyWebReviewFinal() {
    if (!exam || !questions) return;
    const result = validateWebReviewJson(webReviewFinalJson);
    if (!result.ok) {
      setWebReviewResult(`적용 차단\n${result.errors.join("\n")}`);
      return;
    }

    if (
      !confirm(
        `최종 JSON ${result.payload.length}문항을 현재 모의고사에 일괄 적용합니다.\n변경 문항: ${result.changedCount}개\n계속하시겠습니까?`,
      )
    ) {
      return;
    }

    setWebReviewSaving(true);
    setWebReviewResult(null);
    let successCount = 0;
    const failures: string[] = [];

    await Promise.all(
      result.payload.map(async (item) => {
        try {
          const isMcq = item.questionType === "MCQ";
          await updateQuestion(item.id, {
            content: item.content,
            questionType: item.questionType,
            correctOption: isMcq ? item.correctOption : null,
            answer: isMcq ? null : item.answer,
            keywords: isMcq ? null : item.keywords,
            explanation: item.explanation,
            summary: item.summary,
          });
          successCount++;
        } catch (e) {
          failures.push(`ID ${item.id}: ${e instanceof Error ? e.message : "저장 실패"}`);
        }
      }),
    );

    let marked = 0;
    if (failures.length === 0) {
      try {
        const markResult = await markMockExamVerified(examId);
        marked = markResult.marked;
      } catch (e) {
        failures.push(`검수 완료 처리 실패: ${e instanceof Error ? e.message : "알 수 없는 오류"}`);
      }
    }

    try {
      const fulls = await Promise.all(
        exam.questions
          .slice()
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .map((q) => getQuestion(q.id)),
      );
      setQuestions(fulls);
      setWebReviewOriginalJson(JSON.stringify(buildWebReviewQuestions(fulls), null, 2));
    } catch {
      /* ignore */
    }

    setWebReviewResult(
      `${successCount}문항 적용 완료` +
        (marked > 0 ? `, ${marked}문항 검수 완료 처리` : "") +
        (failures.length > 0 ? `\n\n실패 ${failures.length}건:\n${failures.join("\n")}` : ""),
    );
    setWebReviewSaving(false);
  }

  function openJsonEditor() {
    if (!questions) return;
    const data = questions.map((q, i) => ({
      order: i + 1,
      id: q.id,
      subjectName: q.subjectName,
      questionType: q.questionType,
      content: q.content,
      correctOption: q.correctOption,
      answer: q.answer,
      keywords: q.keywords,
      explanation: q.explanation,
      summary: q.summary,
      verified: !!q.verifiedAt,
    }));
    setJsonText(JSON.stringify(data, null, 2));
    setJsonMode(true);
    setJsonResult(null);
  }

  async function handleJsonSave() {
    if (!questions) return;
    let parsed: Array<{ id: number; content: string; questionType: string; correctOption?: number | null; answer?: string | null; keywords?: string[] | null; explanation: string; summary?: string | null }>;
    try {
      parsed = JSON.parse(jsonText);
      if (!Array.isArray(parsed)) throw new Error("배열이 아닙니다");
    } catch (e) {
      setJsonResult(`JSON 파싱 실패: ${e instanceof Error ? e.message : "형식 오류"}`);
      return;
    }

    setJsonSaving(true);
    setJsonResult(null);
    let successCount = 0;
    const failures: string[] = [];

    await Promise.all(
      parsed.map(async (item) => {
        const existing = questions.find((q) => q.id === item.id);
        if (!existing) {
          failures.push(`ID ${item.id}: 해당 문제를 찾을 수 없음`);
          return;
        }
        try {
          const isMcq = (item.questionType ?? existing.questionType) === "MCQ";
          await updateQuestion(item.id, {
            content: item.content,
            questionType: (item.questionType ?? existing.questionType) as import("@/lib/adminApi").AdminQuestionType,
            correctOption: isMcq ? (item.correctOption ?? existing.correctOption) : null,
            answer: isMcq ? null : (item.answer ?? null),
            keywords: isMcq ? null : (item.keywords ?? null),
            explanation: item.explanation,
            summary: item.summary ?? null,
          });
          successCount++;
        } catch (e) {
          failures.push(`ID ${item.id}: ${e instanceof Error ? e.message : "저장 실패"}`);
        }
      }),
    );

    // 문제 목록 새로고침
    if (exam) {
      try {
        const fulls = await Promise.all(
          exam.questions
            .slice()
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((q) => getQuestion(q.id)),
        );
        setQuestions(fulls);
      } catch { /* ignore */ }
    }

    const msg = `${successCount}개 저장 완료` + (failures.length > 0 ? `, ${failures.length}개 실패:\n${failures.join("\n")}` : "");
    setJsonResult(msg);
    setJsonSaving(false);
  }

  function handleQuestionUpdated(updated: AdminQuestion) {
    setQuestions((prev) =>
      prev ? prev.map((q) => (q.id === updated.id ? updated : q)) : prev,
    );
  }

  if (error) {
    return (
      <div className="text-red-400">{error}</div>
    );
  }
  if (!exam || !questions) {
    return <div className="text-muted">로딩 중...</div>;
  }

  const webReviewAutomationPackage = buildWebReviewAutomationPackage();
  const webReviewAutomationPackageText = JSON.stringify(webReviewAutomationPackage, null, 2);
  const webReviewCodexRunCommand = buildWebReviewCodexRunCommand(webReviewAutomationPackage);

  return (
    <div>
      <div className="flex items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <Link
              href="/admin/mock-exams"
              className="text-xs text-muted hover:text-foreground"
            >
              ← 목록
            </Link>
            {prevExam && (
              <Link
                href={`/admin/mock-exams/${prevExam.id}`}
                className="text-xs text-muted hover:text-foreground"
              >
                ← #{prevExam.sequence}
              </Link>
            )}
            {nextExam && (
              <Link
                href={`/admin/mock-exams/${nextExam.id}`}
                className="text-xs text-muted hover:text-foreground"
              >
                #{nextExam.sequence} →
              </Link>
            )}
          </div>
          <h1 className="mt-1 text-2xl font-bold">
            {exam.name}
            {exam.expertVerified && (
              <span className="ml-2 inline-flex items-center rounded-full border border-emerald-500/50 bg-emerald-500/15 px-2 py-0.5 align-middle text-[10px] font-bold text-emerald-300">
                전문가 검수 완료
              </span>
            )}
          </h1>
          <p className="mt-1 text-sm text-muted">
            {exam.examType} · {exam.totalQuestions}문항 · 회차 #{exam.sequence}
          </p>
        </div>
        <div className="flex flex-col items-end gap-2">
          <div className="flex gap-2">
            <button
              data-testid="open-web-review"
              onClick={openWebReview}
              className="rounded border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 text-xs font-medium text-emerald-300 transition hover:bg-emerald-500/20"
            >
              웹 LLM 검수
            </button>
            <button
              onClick={handleMarkVerified}
              className="rounded border border-amber-500/40 bg-amber-500/10 px-3 py-1.5 text-xs font-medium text-amber-300 transition hover:bg-amber-500/20"
            >
              전체 검수 완료
            </button>
            <button
              onClick={async () => {
                const result = await toggleExpertVerified(examId);
                setExam({ ...exam, expertVerified: result.expertVerified });
              }}
              className={`rounded border px-3 py-1.5 text-xs font-medium transition ${
                exam.expertVerified
                  ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20"
                  : "border-border text-muted hover:text-foreground"
              }`}
            >
              {exam.expertVerified ? "전문가 검수 해제" : "전문가 검수 완료"}
            </button>
            <button
              onClick={openJsonEditor}
              className="rounded border border-violet-500/40 bg-violet-500/10 px-3 py-1.5 text-xs font-medium text-violet-300 transition hover:bg-violet-500/20"
            >
              JSON 편집
            </button>
            <button
              onClick={() => downloadJson(exam, questions)}
              className="rounded border border-border px-3 py-1.5 text-xs text-muted transition hover:text-foreground"
            >
              JSON
            </button>
            <button
              onClick={() => downloadMd(exam, questions)}
              className="rounded border border-border px-3 py-1.5 text-xs text-muted transition hover:text-foreground"
            >
              MD
            </button>
            <button
              onClick={async () => {
                setPdfBusy(true);
                setPdfStatus("PDF 준비 중…");
                try {
                  await downloadMockExamPdf(examId);
                  setPdfStatus("PDF 다운로드 시작");
                } catch (e) {
                  setPdfStatus(e instanceof Error ? `실패: ${e.message}` : "PDF 다운로드 실패");
                } finally {
                  setPdfBusy(false);
                }
              }}
              disabled={pdfBusy}
              className="rounded border border-rose-500/40 bg-rose-500/10 px-3 py-1.5 text-xs font-medium text-rose-300 transition hover:bg-rose-500/20 disabled:opacity-50"
              title="동일 콘텐츠는 R2 캐시에서 즉시 반환. 새로 생성 시 5~15초 소요."
            >
              {pdfBusy ? "PDF 생성 중…" : "PDF 다운로드"}
            </button>
          </div>
          {verifyResult && (
            <p className="text-xs text-muted">{verifyResult}</p>
          )}
          {pdfStatus && (
            <p className="text-xs text-muted">{pdfStatus}</p>
          )}
        </div>
      </div>

      <PastExamMetaPanel
        examId={examId}
        exam={exam}
        onUpdated={(next) => setExam({ ...exam, ...next })}
      />

      {webReviewOpen && (
        <div
          data-testid="web-review-panel"
          className="mt-6 rounded-xl border border-emerald-500/30 bg-surface p-4"
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-emerald-300">웹 LLM 검수</h2>
              <p className="mt-1 text-xs text-muted">
                Claude에는 원본 JSON만, ChatGPT에는 Claude 수정본 JSON만 그대로 전달합니다.
              </p>
            </div>
            <button
              type="button"
              onClick={() => {
                setWebReviewOpen(false);
                setWebReviewResult(null);
              }}
              className="rounded border border-border px-3 py-1.5 text-xs text-muted hover:text-foreground"
            >
              닫기
            </button>
          </div>

          <div className="mt-4 rounded-lg border border-border bg-background p-3">
            <div className="flex flex-wrap items-end gap-3">
              <label className="min-w-[260px] flex-1 text-xs text-muted">
                Claude 프로젝트 URL
                <input
                  data-testid="web-review-claude-url"
                  value={webReviewClaudeProjectUrl}
                  onChange={(e) => setWebReviewClaudeProjectUrl(e.target.value)}
                  placeholder="https://claude.ai/project/..."
                  className="mt-1 w-full rounded border border-border bg-surface px-3 py-2 text-sm text-foreground"
                />
              </label>
              <label className="min-w-[260px] flex-1 text-xs text-muted">
                ChatGPT 프로젝트 URL
                <input
                  data-testid="web-review-chatgpt-url"
                  value={webReviewChatgptProjectUrl}
                  onChange={(e) => setWebReviewChatgptProjectUrl(e.target.value)}
                  placeholder="https://chatgpt.com/g/..."
                  className="mt-1 w-full rounded border border-border bg-surface px-3 py-2 text-sm text-foreground"
                />
              </label>
              <button
                type="button"
                data-testid="web-review-save-project-urls"
                onClick={saveWebReviewProjectUrls}
                className="rounded border border-emerald-500/40 bg-emerald-500/10 px-3 py-2 text-xs font-medium text-emerald-300 hover:bg-emerald-500/20"
              >
                URL 저장
              </button>
              <button
                type="button"
                data-testid="web-review-copy-automation-package"
                onClick={copyWebReviewAutomationPackage}
                disabled={webReviewAutomationPackage.status !== "ready"}
                className="rounded border border-cyan-500/40 bg-cyan-500/10 px-3 py-2 text-xs font-medium text-cyan-300 hover:bg-cyan-500/20 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Codex 자동 실행 패키지 복사
              </button>
            </div>
          </div>

          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            <Field label="Codex 자동 실행 명령">
              <textarea
                data-testid="web-review-codex-run-command"
                value={webReviewCodexRunCommand}
                readOnly
                rows={8}
                className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
                spellCheck={false}
              />
              <button
                type="button"
                data-testid="web-review-copy-codex-run-command"
                onClick={() => copyWebReviewText(webReviewCodexRunCommand, "Codex 자동 실행 명령")}
                className="mt-2 rounded border border-cyan-500/40 bg-cyan-500/10 px-3 py-1.5 text-xs font-medium text-cyan-300 hover:bg-cyan-500/20"
              >
                실행 명령 복사
              </button>
            </Field>

            <Field label="Codex 자동 실행 패키지">
              <textarea
                data-testid="web-review-automation-package"
                value={webReviewAutomationPackageText}
                readOnly
                rows={8}
                className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
                spellCheck={false}
              />
              {webReviewAutomationPackage.status === "blocked" && (
                <pre className="mt-2 whitespace-pre-wrap rounded border border-amber-500/30 bg-amber-500/5 px-3 py-2 text-xs text-amber-300">
                  {webReviewAutomationPackage.errors.join("\n")}
                </pre>
              )}
            </Field>
          </div>

          <div className="mt-4 grid gap-4 xl:grid-cols-3">
            <Field label="1. Claude에 보낼 원본 JSON">
              <textarea
                data-testid="web-review-original-json"
                value={webReviewOriginalJson}
                readOnly
                rows={18}
                className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
                spellCheck={false}
              />
              <button
                type="button"
                data-testid="web-review-copy-original-json"
                onClick={() => copyWebReviewText(webReviewOriginalJson, "원본 JSON")}
                className="mt-2 rounded border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 text-xs font-medium text-emerald-300 hover:bg-emerald-500/20"
              >
                원본 JSON 복사
              </button>
            </Field>

            <Field label="2. Claude 전체 수정본 JSON">
              <textarea
                data-testid="web-review-claude-json"
                value={webReviewClaudeJson}
                onChange={(e) => {
                  setWebReviewClaudeJson(e.target.value);
                  setWebReviewResult(null);
                }}
                rows={18}
                className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
                spellCheck={false}
                placeholder="Claude 프로젝트가 반환한 전체 JSON을 붙여넣으세요."
              />
              <button
                type="button"
                data-testid="web-review-copy-claude-json"
                onClick={() => copyWebReviewText(webReviewClaudeJson, "Claude 수정본 JSON")}
                className="mt-2 rounded border border-cyan-500/40 bg-cyan-500/10 px-3 py-1.5 text-xs font-medium text-cyan-300 hover:bg-cyan-500/20"
              >
                ChatGPT 전달용 복사
              </button>
            </Field>

            <Field label="3. ChatGPT 최종 JSON">
              <textarea
                data-testid="web-review-final-json"
                value={webReviewFinalJson}
                onChange={(e) => {
                  setWebReviewFinalJson(e.target.value);
                  setWebReviewResult(null);
                }}
                rows={18}
                className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
                spellCheck={false}
                placeholder="ChatGPT 프로젝트가 반환한 최종 전체 JSON을 붙여넣으세요."
              />
              <div className="mt-2 flex flex-wrap gap-2">
                <button
                  type="button"
                  data-testid="web-review-validate-final"
                  onClick={handleValidateWebReviewFinal}
                  className="rounded border border-border px-3 py-1.5 text-xs text-muted hover:text-foreground"
                >
                  최종 JSON 검증
                </button>
                <button
                  type="button"
                  data-testid="web-review-apply-final"
                  onClick={handleApplyWebReviewFinal}
                  disabled={webReviewSaving}
                  className="rounded bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-black hover:bg-emerald-400 disabled:opacity-50"
                >
                  {webReviewSaving ? "적용 중..." : "최종 JSON 일괄 적용"}
                </button>
              </div>
            </Field>
          </div>

          {webReviewResult && (
            <pre className="mt-4 max-h-72 overflow-y-auto whitespace-pre-wrap rounded border border-border bg-background px-3 py-2 text-xs text-muted">
              {webReviewResult}
            </pre>
          )}
        </div>
      )}

      {jsonMode && (
        <div className="mt-6 rounded-xl border border-violet-500/30 bg-surface p-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-violet-300">JSON 일괄 편집</h2>
            <div className="flex gap-2">
              <button
                onClick={handleJsonSave}
                disabled={jsonSaving}
                className="rounded bg-violet-500 px-4 py-1.5 text-xs font-medium text-white hover:bg-violet-600 disabled:opacity-50"
              >
                {jsonSaving ? "저장 중..." : "JSON 저장"}
              </button>
              <button
                onClick={() => { setJsonMode(false); setJsonResult(null); }}
                className="rounded border border-border px-3 py-1.5 text-xs text-muted hover:text-foreground"
              >
                닫기
              </button>
            </div>
          </div>
          <textarea
            value={jsonText}
            onChange={(e) => setJsonText(e.target.value)}
            rows={30}
            className="mt-3 w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
            spellCheck={false}
          />
          {jsonResult && (
            <pre className="mt-2 whitespace-pre-wrap text-xs text-muted">{jsonResult}</pre>
          )}
        </div>
      )}

      <div className="mt-6 space-y-4">
        {questions.map((q, idx) => (
          <QuestionEditCard
            key={q.id}
            order={idx + 1}
            initial={q}
            onUpdated={handleQuestionUpdated}
          />
        ))}
      </div>
    </div>
  );
}

// ===========================================================
// 문제 카드 — 인라인 편집
// ===========================================================

function QuestionEditCard({
  order,
  initial,
  onUpdated,
}: {
  order: number;
  initial: AdminQuestion;
  onUpdated: (q: AdminQuestion) => void;
}) {
  const isMcq = initial.questionType === "MCQ";
  const initialKeywordsText = (initial.keywords ?? []).join(", ");

  const [content, setContent] = useState(initial.content);
  const [correctOption, setCorrectOption] = useState<number>(initial.correctOption ?? 1);
  const [answer, setAnswer] = useState(initial.answer ?? "");
  const [keywordsText, setKeywordsText] = useState(initialKeywordsText);
  const [explanation, setExplanation] = useState(initial.explanation);
  const [summary, setSummary] = useState(initial.summary ?? "");
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const dirty =
    content !== initial.content ||
    (isMcq && correctOption !== (initial.correctOption ?? 1)) ||
    (!isMcq && answer !== (initial.answer ?? "")) ||
    (!isMcq && keywordsText !== initialKeywordsText) ||
    explanation !== initial.explanation ||
    (summary || null) !== (initial.summary || null);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      const keywords = keywordsText
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
      const updated = await updateQuestion(initial.id, {
        content,
        questionType: initial.questionType,
        correctOption: isMcq ? correctOption : null,
        answer: isMcq ? null : (answer || null),
        keywords: isMcq ? null : (keywords.length > 0 ? keywords : null),
        explanation,
        summary: summary.trim() ? summary : null,
      });
      onUpdated(updated);
      setSavedAt(Date.now());
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setSaving(false);
    }
  }

  function handleReset() {
    setContent(initial.content);
    setCorrectOption(initial.correctOption ?? 1);
    setAnswer(initial.answer ?? "");
    setKeywordsText(initialKeywordsText);
    setExplanation(initial.explanation);
    setSummary(initial.summary ?? "");
    setError(null);
  }

  return (
    <div className="rounded-xl border border-border bg-surface p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-sm">
          <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-300">
            #{order}
          </span>
          <span className="rounded bg-zinc-500/10 px-2 py-0.5 text-xs text-muted">
            ID {initial.id}
          </span>
          <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs text-violet-300">
            {initial.questionType}
          </span>
          <span className="text-xs text-muted">{initial.subjectName}</span>
        </div>
        <div className="flex items-center gap-2">
          {savedAt && !dirty && (
            <span className="text-xs text-emerald-400">✓ 저장됨</span>
          )}
          {dirty && (
            <button
              onClick={handleReset}
              disabled={saving}
              className="rounded border border-border px-3 py-1 text-xs text-muted hover:text-foreground disabled:opacity-50"
            >
              되돌리기
            </button>
          )}
          <button
            onClick={handleSave}
            disabled={!dirty || saving}
            className="rounded bg-violet-500 px-3 py-1 text-xs font-medium text-white hover:bg-violet-600 disabled:opacity-50"
          >
            {saving ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>

      <div className="mt-3 space-y-3">
        <Field label="문제 (content)">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={Math.min(20, Math.max(6, content.split("\n").length + 1))}
            className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
          />
        </Field>

        {isMcq ? (
          <div className="grid gap-3 sm:grid-cols-[120px_1fr]">
            <Field label="정답 옵션">
              <select
                value={correctOption}
                onChange={(e) => setCorrectOption(Number(e.target.value))}
                className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
              >
                {[1, 2, 3, 4].map((n) => (
                  <option key={n} value={n}>
                    {n}번
                  </option>
                ))}
              </select>
            </Field>
            <Field label="요약 (summary)">
              <input
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="(선택)"
                className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
              />
            </Field>
          </div>
        ) : (
          <>
            <Field label="정답 (answer)">
              <textarea
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                rows={Math.min(8, Math.max(2, answer.split("\n").length + 1))}
                className="w-full rounded border border-border bg-background px-3 py-2 text-xs"
              />
            </Field>
            <Field label="키워드 / Alias (쉼표 구분 — 단답형은 alias, 약술형은 채점 키워드)">
              <input
                value={keywordsText}
                onChange={(e) => setKeywordsText(e.target.value)}
                placeholder="예: OTU, O T U"
                className="w-full rounded border border-border bg-background px-3 py-2 text-xs"
              />
            </Field>
            <Field label="요약 (summary)">
              <input
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="(선택)"
                className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
              />
            </Field>
          </>
        )}

        <Field label="해설 (explanation)">
          <textarea
            value={explanation}
            onChange={(e) => setExplanation(e.target.value)}
            rows={Math.min(15, Math.max(4, explanation.split("\n").length + 1))}
            className="w-full rounded border border-border bg-background px-3 py-2 text-xs"
          />
        </Field>
      </div>

      {error && (
        <div className="mt-3 rounded border border-red-500/30 bg-red-500/5 px-3 py-2 text-xs text-red-400">
          {error}
        </div>
      )}
    </div>
  );
}

function triggerDownload(content: string, filename: string, mime: string) {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function downloadJson(exam: AdminMockExamDetail, questions: AdminQuestion[]) {
  const data = {
    examName: exam.name,
    examType: exam.examType,
    sequence: exam.sequence,
    totalQuestions: exam.totalQuestions,
    questions: questions.map((q, i) => ({
      order: i + 1,
      id: q.id,
      subjectName: q.subjectName,
      questionType: q.questionType,
      content: q.content,
      correctOption: q.correctOption,
      answer: q.answer,
      keywords: q.keywords,
      explanation: q.explanation,
      summary: q.summary,
      verified: !!q.verifiedAt,
    })),
  };
  const filename = `${exam.name.replace(/\s+/g, "_")}_${exam.examType}.json`;
  triggerDownload(JSON.stringify(data, null, 2), filename, "application/json");
}

function downloadMd(exam: AdminMockExamDetail, questions: AdminQuestion[]) {
  const lines: string[] = [];
  lines.push(`# ${exam.name}`);
  lines.push(`- 시험 유형: ${exam.examType}`);
  lines.push(`- 총 문항: ${exam.totalQuestions}`);
  lines.push(`- 회차: ${exam.sequence}`);
  lines.push("");

  questions.forEach((q, i) => {
    lines.push(`---`);
    lines.push(`## 문항 ${i + 1} (ID: ${q.id})`);
    lines.push(`- 과목: ${q.subjectName}`);
    lines.push(`- 유형: ${q.questionType}`);
    if (q.summary) lines.push(`- 요약: ${q.summary}`);
    lines.push(`- 검수: ${q.verifiedAt ? "완료" : "미검수"}`);
    lines.push("");
    lines.push("### 문제");
    lines.push(q.content);
    lines.push("");
    if (q.questionType === "MCQ" && q.correctOption) {
      lines.push(`### 정답: ${q.correctOption}번`);
    } else if (q.answer) {
      lines.push(`### 정답`);
      lines.push(q.answer);
    }
    if (q.keywords && q.keywords.length > 0) {
      lines.push(`### 키워드: ${q.keywords.join(", ")}`);
    }
    lines.push("");
    lines.push("### 해설");
    lines.push(q.explanation);
    lines.push("");
  });

  const filename = `${exam.name.replace(/\s+/g, "_")}_${exam.examType}.md`;
  triggerDownload(lines.join("\n"), filename, "text/markdown");
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-muted">
        {label}
      </span>
      {children}
    </label>
  );
}

// ===========================================================
// 기출 복원 메타 — kind/연도/회차/시험일 설정 패널
// ===========================================================

function PastExamMetaPanel({
  examId,
  exam,
  onUpdated,
}: {
  examId: number;
  exam: AdminMockExamDetail;
  onUpdated: (next: Partial<AdminMockExamDetail>) => void;
}) {
  const [year, setYear] = useState<string>(exam.examYear ? String(exam.examYear) : "");
  const [round, setRound] = useState<string>(exam.examRound ? String(exam.examRound) : "");
  const [date, setDate] = useState<string>(exam.examDate ?? "");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const isPast = exam.kind === "PAST_EXAM";

  async function save(promote: boolean) {
    setSaving(true);
    setMessage(null);
    try {
      const yearNum = year.trim() ? Number(year) : null;
      const roundNum = round.trim() ? Number(round) : null;
      const updated = await setPastExamMeta(examId, {
        promote,
        examYear: yearNum,
        examRound: roundNum,
        examDate: date.trim() ? date : null,
      });
      onUpdated({
        kind: updated.kind,
        examYear: updated.examYear,
        examRound: updated.examRound,
        examDate: updated.examDate,
      });
      setMessage(promote ? "기출 복원으로 설정됨" : "AI 모의고사로 되돌림");
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mt-6 rounded-xl border border-border bg-surface p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h2 className="text-sm font-semibold">기출 복원 메타</h2>
          <span
            className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
              isPast
                ? "border border-amber-500/50 bg-amber-500/15 text-amber-300"
                : "border border-border text-muted"
            }`}
          >
            {isPast ? "PAST_EXAM" : "AI"}
          </span>
        </div>
        {message && <span className="text-xs text-muted">{message}</span>}
      </div>

      <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-4">
        <Field label="연도">
          <input
            type="number"
            value={year}
            onChange={(e) => setYear(e.target.value)}
            placeholder="2025"
            className="w-full rounded border border-border bg-background px-3 py-1.5 text-sm"
          />
        </Field>
        <Field label="회차">
          <input
            type="number"
            value={round}
            onChange={(e) => setRound(e.target.value)}
            placeholder="59"
            className="w-full rounded border border-border bg-background px-3 py-1.5 text-sm"
          />
        </Field>
        <Field label="시험일 (선택)">
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full rounded border border-border bg-background px-3 py-1.5 text-sm"
          />
        </Field>
        <div className="flex items-end gap-2">
          <button
            onClick={() => save(true)}
            disabled={saving}
            className="flex-1 rounded border border-amber-500/40 bg-amber-500/10 px-3 py-1.5 text-xs font-semibold text-amber-300 transition hover:bg-amber-500/20 disabled:opacity-50"
          >
            기출로 저장
          </button>
          {isPast && (
            <button
              onClick={() => save(false)}
              disabled={saving}
              className="flex-1 rounded border border-border px-3 py-1.5 text-xs text-muted transition hover:text-foreground disabled:opacity-50"
            >
              AI 로 되돌리기
            </button>
          )}
        </div>
      </div>
      <p className="mt-2 text-[11px] text-muted">
        PUBLISHED + 전문가 검수 완료 상태여야 <code className="text-amber-300">/past-exams</code> 에 노출됩니다.
      </p>
    </div>
  );
}
