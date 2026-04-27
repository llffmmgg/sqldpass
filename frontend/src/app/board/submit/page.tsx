"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";

import AuthGuard from "@/components/AuthGuard";
import { Button, Container } from "@/components/ui";
import { CERT_LIST, type CertKey } from "@/lib/cert-tokens";
import { submitPost } from "@/lib/api";

export default function SubmitPostPage() {
  return (
    <AuthGuard>
      <SubmitPostContent />
    </AuthGuard>
  );
}

function SubmitPostContent() {
  const router = useRouter();
  const [cert, setCert] = useState<CertKey | "">("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = !!cert && title.trim().length > 0 && content.trim().length > 0;

  async function handleSubmit() {
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      await submitPost({
        category: "PASS_REVIEW",
        cert: cert as string,
        title: title.trim(),
        content: content.trim(),
      });
      router.push("/board/submit/done");
    } catch (e) {
      setError(e instanceof Error ? e.message : "제출에 실패했습니다.");
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-12">
        <Link href="/board" className="text-sm text-text-muted hover:text-text">
          ← 게시판
        </Link>

        <h1 className="mt-4 text-2xl font-bold tracking-tight sm:text-3xl">
          🎉 합격 후기 제출하기
        </h1>
        <p className="mt-2 text-sm text-text-muted">
          제출해주신 후기는 운영자 검토 후 게시됩니다 (보통 1~2일 이내).
        </p>

        <div className="mt-8 space-y-5 rounded-xl border border-border bg-surface p-6">
          {/* 자격증 */}
          <div>
            <label className="block text-sm font-semibold">자격증</label>
            <select
              value={cert}
              onChange={(e) => setCert(e.target.value as CertKey | "")}
              className="mt-2 w-full rounded-md border border-border bg-bg-elevated px-3 py-2 text-sm text-text focus:border-primary focus:outline-none"
            >
              <option value="">자격증을 선택하세요</option>
              {CERT_LIST.map((c) => (
                <option key={c.key} value={c.key}>
                  {c.labelLong}
                </option>
              ))}
            </select>
          </div>

          {/* 제목 */}
          <div>
            <label className="block text-sm font-semibold">제목</label>
            <input
              type="text"
              value={title}
              maxLength={120}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예: 비전공자 3개월 SQLD 합격 후기"
              className="mt-2 w-full rounded-md border border-border bg-bg-elevated px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
            />
            <p className="mt-1 text-right text-[11px] text-text-subtle tabular-nums">
              {title.length}/120
            </p>
          </div>

          {/* 본문 */}
          <div>
            <label className="block text-sm font-semibold">본문</label>
            <textarea
              value={content}
              rows={14}
              onChange={(e) => setContent(e.target.value)}
              placeholder={[
                "공부 기간, 사용한 교재·강의, 점수, 도움된 팁 등을 자유롭게 적어주세요.",
                "",
                "예시 항목:",
                "- 응시일·점수",
                "- 학습 기간 / 하루 평균 공부 시간",
                "- 사용한 교재·강의·문어CBT 활용법",
                "- 시험 직전 1주일 전략",
                "- 후배에게 한마디",
              ].join("\n")}
              className="mt-2 w-full resize-y rounded-md border border-border bg-bg-elevated px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
            />
          </div>

          {error && (
            <div className="rounded-md border border-red-500/30 bg-red-500/5 px-3 py-2 text-sm text-red-400">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-2">
            <Link
              href="/board"
              className="inline-flex items-center rounded-md border border-border bg-surface px-4 py-2 text-sm text-text-muted hover:border-border-strong hover:text-text"
            >
              취소
            </Link>
            <Button
              variant="primary"
              size="md"
              loading={submitting}
              disabled={!canSubmit}
              onClick={handleSubmit}
            >
              제출하기
            </Button>
          </div>
        </div>
      </Container>
    </main>
  );
}
