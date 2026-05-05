"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";

import { Badge, Button, Card } from "@/components/ui";
import Spinner from "@/components/Spinner";
import { useToast } from "@/components/Toast";
import { isLoggedIn } from "@/lib/auth";
import { getCheckoutEligibility, startPayment } from "@/lib/payment";

type AccessState = "loading" | "anonymous" | "denied" | "allowed";

export default function CheckoutClient() {
  return (
    <Suspense fallback={
      <div className="flex min-h-[40vh] items-center justify-center">
        <Spinner message="확인 중..." />
      </div>
    }>
      <CheckoutContent />
    </Suspense>
  );
}

function CheckoutContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const examIdRaw = searchParams?.get("examId");
  const examId = examIdRaw && /^\d+$/.test(examIdRaw) ? Number(examIdRaw) : null;

  const toast = useToast();
  const [access, setAccess] = useState<AccessState>("loading");
  const [paying, setPaying] = useState(false);
  const [paid, setPaid] = useState<{ amount: number; productName: string; mockExamId: number | null } | null>(null);

  useEffect(() => {
    if (!isLoggedIn()) {
      setAccess("anonymous");
      return;
    }
    getCheckoutEligibility()
      .then((r) => setAccess(r.eligible ? "allowed" : "denied"))
      .catch(() => setAccess("denied"));
  }, []);

  if (access === "loading") {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <Spinner message="확인 중..." />
      </div>
    );
  }

  if (access === "anonymous" || access === "denied") {
    return <NotFoundView />;
  }

  async function onPay() {
    if (paying) return;
    setPaying(true);
    try {
      const result = await startPayment({ mockExamId: examId });
      setPaid({ amount: result.amount, productName: result.productName, mockExamId: result.mockExamId });
      toast.show("결제가 완료되었습니다.", "success");
      // 잠금 해제 대상이 명시된 경우 자동으로 풀이 페이지로 이동
      if (result.mockExamId != null) {
        setTimeout(() => router.push(`/mock-exams/${result.mockExamId}`), 800);
      }
    } catch (e) {
      toast.show(
        e instanceof Error ? e.message : "결제 처리 중 오류가 발생했습니다.",
        "error",
      );
    } finally {
      setPaying(false);
    }
  }

  if (paid) {
    return (
      <Card padding="lg" className="text-center">
        <Badge variant="soft" tone="success" size="sm">
          결제 완료
        </Badge>
        <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
          결제가 정상 처리되었습니다
        </h1>
        <p className="mt-3 text-sm text-text-muted">
          {paid.productName} · {paid.amount.toLocaleString()}원
        </p>
        <div className="mt-6 flex items-center justify-center gap-4 text-sm">
          <Link
            href={paid.mockExamId != null ? `/mock-exams/${paid.mockExamId}` : "/mock-exams"}
            className="rounded-lg border border-border bg-surface px-4 py-2 text-text transition-colors hover:border-primary/40"
          >
            {paid.mockExamId != null ? "지금 풀러가기 →" : "모의고사로 →"}
          </Link>
        </div>
      </Card>
    );
  }

  return (
    <Card padding="lg">
      <Badge variant="soft" tone="info" size="sm">
        프리미엄 결제
      </Badge>
      <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
        {examId != null
          ? `프리미엄 모의고사 #${examId} 잠금 해제`
          : "문어CBT 프리미엄 모의고사 잠금 해제"}
      </h1>
      <p className="mt-3 text-sm leading-relaxed text-text-muted">
        결제 완료 시 잠금된 프리미엄 회차를 즉시 풀이할 수 있습니다.
        결제 정보는 PortOne(주식회사 코리아포트원) PG 사를 통해 안전하게 처리되며,
        서버에는 카드 정보가 저장되지 않습니다.
      </p>

      <dl className="mt-6 divide-y divide-border rounded-lg border border-border bg-surface">
        <Row label="상품명" value="문어CBT 프리미엄 모의고사 1회차 잠금 해제" />
        <Row label="결제 금액" value="3,000원" />
        <Row label="결제 수단" value="신용/체크카드 (PortOne)" />
      </dl>

      <div className="mt-6 flex justify-end">
        <Button variant="primary" size="lg" onClick={onPay} disabled={paying}>
          {paying ? "결제 진행 중..." : "결제하기"}
        </Button>
      </div>

      <p className="mt-6 text-[11px] text-text-subtle">
        결제 진행 시{" "}
        <Link href="/terms" className="underline">
          이용약관
        </Link>{" "}
        및{" "}
        <Link href="/refund" className="underline">
          환불 정책
        </Link>
        에 동의하는 것으로 간주됩니다.
      </p>
    </Card>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between px-5 py-3">
      <dt className="text-xs font-medium text-text-muted">{label}</dt>
      <dd className="text-sm font-semibold text-text">{value}</dd>
    </div>
  );
}

function NotFoundView() {
  return (
    <Card padding="lg" className="text-center">
      <p className="text-6xl font-bold text-text-subtle">404</p>
      <h1 className="mt-3 text-xl font-semibold tracking-tight">
        페이지를 찾을 수 없습니다
      </h1>
      <p className="mt-2 text-sm text-text-muted">요청하신 주소가 잘못되었거나 삭제되었습니다.</p>
      <Link
        href="/"
        className="mt-5 inline-block rounded-lg border border-border bg-surface px-4 py-2 text-sm text-text transition-colors hover:border-primary/40"
      >
        홈으로 →
      </Link>
    </Card>
  );
}
