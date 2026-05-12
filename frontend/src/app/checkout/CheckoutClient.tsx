"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";

import { Badge } from "@/components/ui";
import Spinner from "@/components/Spinner";
import { useToast } from "@/components/Toast";
import CheckoutLanding, { planLabel } from "@/components/billing/CheckoutLanding";
import { isLoggedIn } from "@/lib/auth";
import {
  getActiveSubscription,
  getCheckoutEligibility,
  previewPayment,
  startPayment,
  verifyPaymentById,
  type ActiveSubscription,
  type BuyerInfo,
  type PaymentMethod,
  type PreviewResponse,
  type SubscriptionPlan,
} from "@/lib/payment";
import BuyerInfoModal from "@/components/billing/BuyerInfoModal";

type AccessState = "loading" | "anonymous" | "denied" | "allowed";

export default function CheckoutClient() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[40vh] items-center justify-center">
          <Spinner message="확인 중..." />
        </div>
      }
    >
      <CheckoutContent />
    </Suspense>
  );
}

function CheckoutContent() {
  const router = useRouter();
  const toast = useToast();
  const searchParams = useSearchParams();
  // 모바일 redirectUrl 복귀 케이스 — payment.ts 가 ?paymentId=... 로 돌려보냄.
  // 데스크탑은 SDK Promise resolve 로 onPay 안에서 verify 함 → 이 쿼리는 비어있음.
  const returnedPaymentId = searchParams.get("paymentId");
  const [access, setAccess] = useState<AccessState>("loading");
  const [subscription, setSubscription] = useState<ActiveSubscription | null>(null);
  const [payingPlan, setPayingPlan] = useState<SubscriptionPlan | null>(null);
  const [method, setMethod] = useState<PaymentMethod>("CARD");
  // 결제 모달 — onPay 클릭 시 plan 을 세팅하면 모달 열림. onBuyerSubmit 에서 실제 결제 호출.
  const [pendingPlan, setPendingPlan] = useState<SubscriptionPlan | null>(null);
  // 결제수단 토글 임시 숨김 — 카카오페이 채널 운영 보류, CARD 단일 흐름.
  // 재오픈 시: `!isCapacitorApp()` 로 되돌리고 BuyerInfoModal/PG 분기 검토.
  const showMethodToggle = false;
  // plan 별 미리보기 — 활성 구독 prorate 차감 적용된 finalAmount 표시용
  const [previews, setPreviews] = useState<Record<SubscriptionPlan, PreviewResponse | null>>({
    THREE_DAY: null,
    ONE_MONTH: null,
    UNLIMITED: null,
  });

  useEffect(() => {
    if (!isLoggedIn()) {
      setAccess("anonymous");
      return;
    }
    Promise.all([getCheckoutEligibility(), getActiveSubscription()])
      .then(([elig, sub]) => {
        setAccess(elig.eligible ? "allowed" : "denied");
        setSubscription(sub);

        // 활성 구독이 있으면 plan 별 미리보기 호출 (prorate 표시용).
        // 비활성이면 baseAmount 그대로 표시되니 호출 생략 (네트워크 절약).
        if (elig.eligible && sub.active) {
          (["THREE_DAY", "ONE_MONTH", "UNLIMITED"] as const).forEach((plan) => {
            previewPayment(plan)
              .then((p) => setPreviews((prev) => ({ ...prev, [plan]: p })))
              .catch(() => {
                // 개별 plan 미리보기 실패는 무시 — 카드는 baseAmount 로 fallback
              });
          });
        }
      })
      .catch(() => setAccess("denied"));
  }, []);

  // 같은 paymentId 에 대해 verify 가 두 번 이상 안 돌아가게 한다.
  // 모바일 redirectUrl 복귀 후 Toast/router 등 deps 가 흔들려도 1회만 처리.
  const verifiedRef = useRef<string | null>(null);

  // 모바일 PG 외부 앱 복귀 시 redirectUrl 로 들어온 paymentId 를 즉시 검증.
  // verify 멱등 보증이라 SDK Promise 경로와 동시에 와도 안전.
  useEffect(() => {
    if (!returnedPaymentId) return;
    if (verifiedRef.current === returnedPaymentId) return;
    verifiedRef.current = returnedPaymentId;
    verifyPaymentById(returnedPaymentId)
      .then((result) => {
        toast.show(`${planLabel(result.plan)} 결제 완료`, "success");
        router.replace("/checkout");
        setTimeout(() => router.push("/mock-exams"), 800);
      })
      .catch((e) => {
        const message = e instanceof Error ? e.message : "";
        const cancelled = /취소|cancel/i.test(message);
        toast.show(
          cancelled ? "결제를 취소하셨습니다." : message || "결제 검증에 실패했습니다.",
          cancelled ? "info" : "error",
        );
        router.replace("/checkout");
        if (e instanceof Error) console.error("[checkout:return]", e);
      });
    // router/toast 는 stable hook 결과로 다뤄야 한다. deps 에 추가하면 매 렌더 effect 재실행
    // → verify 중복 호출 사고 재발. verifiedRef 가드도 함께 2중 방어.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [returnedPaymentId]);

  if (access === "loading") {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <Spinner message="확인 중..." />
      </div>
    );
  }

  if (access === "anonymous") {
    return <ComingSoonView showLogin />;
  }
  if (access === "denied") {
    return <ComingSoonView showLogin={false} />;
  }

  // 결제 버튼 클릭 — BuyerInfoModal 을 열고, 실제 결제는 onBuyerSubmit 에서 진행.
  function onPay(plan: SubscriptionPlan) {
    if (payingPlan) return;
    setPendingPlan(plan);
  }

  async function onBuyerSubmit(buyer: BuyerInfo) {
    if (!pendingPlan) return;
    const plan = pendingPlan;
    setPendingPlan(null);
    setPayingPlan(plan);
    try {
      const result = await startPayment({ plan, method, buyer });
      toast.show(`${planLabel(result.plan)} 결제 완료`, "success");
      setTimeout(() => router.push("/mock-exams"), 800);
    } catch (e) {
      const message = e instanceof Error ? e.message : "";
      // PortOne SDK 의 사용자 취소는 message 안에 "취소" / "cancel" 키워드 포함 — info 톤
      const cancelled = /취소|cancel/i.test(message);
      if (cancelled) {
        toast.show("결제를 취소하셨습니다.", "info");
      } else {
        // 그 외는 백엔드 검증 실패 / 네트워크 오류 — 일반 사용자 친화 메시지로
        toast.show(
          message || "결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
          "error",
        );
      }
      if (e instanceof Error) console.error("[checkout]", e);
    } finally {
      setPayingPlan(null);
    }
  }

  return (
    <>
      <CheckoutLanding
        currentPlan={subscription?.active ? subscription.plan : null}
        previews={previews}
        payingPlan={payingPlan}
        subscription={subscription}
        method={method}
        onChangeMethod={setMethod}
        showMethodToggle={showMethodToggle}
        onPay={onPay}
      />
      {pendingPlan !== null && (
        <BuyerInfoModal
          open
          plan={pendingPlan}
          onClose={() => setPendingPlan(null)}
          onSubmit={onBuyerSubmit}
        />
      )}
    </>
  );
}

function ComingSoonView({ showLogin }: { showLogin: boolean }) {
  return (
    <div className="mx-auto max-w-md text-center">
      <div className="rounded-2xl border border-border bg-surface/40 p-10">
        <Badge variant="soft" tone="info" size="sm">
          준비 중
        </Badge>
        <h1 className="mt-4 text-2xl font-bold tracking-tight">
          결제 페이지는 곧 오픈됩니다
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-text-muted">
          지금은 베타 오픈 단계라 일부 운영자 계정에서만 결제가 가능합니다.
          <br />
          정식 오픈 안내는 사이트 공지사항으로 알려드릴게요.
        </p>
        <div className="mt-6 flex flex-col items-center gap-3 text-sm">
          {showLogin && (
            <p className="text-xs text-text-subtle">로그인이 필요할 수도 있습니다.</p>
          )}
          <Link
            href="/mock-exams"
            className="rounded-lg border border-border bg-surface px-5 py-2.5 text-text transition-colors hover:border-primary/40"
          >
            모의고사로 →
          </Link>
        </div>
      </div>
    </div>
  );
}
