import type { Metadata } from "next";
import CheckoutClient from "./CheckoutClient";

export const metadata: Metadata = {
  title: "결제",
  description: "문어CBT 고난이도 모의고사 결제 페이지.",
  robots: { index: false, follow: false, nocache: true },
  alternates: { canonical: "/checkout" },
};

export default function CheckoutPage() {
  return (
    <main className="min-h-screen bg-bg text-text">
      {/* 결제 페이지 한정 — 5컬럼 카드가 좁아 보여서 1300px 까지 넓힘 (다른 페이지 영향 X) */}
      <div className="mx-auto w-full max-w-[1300px] px-4 py-20 sm:px-6 lg:px-8">
        <CheckoutClient />
      </div>
    </main>
  );
}
