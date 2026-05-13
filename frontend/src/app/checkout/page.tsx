import type { Metadata } from "next";
import { Container } from "@/components/ui";
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
      <Container size="wide" className="py-20">
        <CheckoutClient />
      </Container>
    </main>
  );
}
