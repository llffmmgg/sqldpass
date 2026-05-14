import type { Metadata } from "next";
import { Container } from "@/components/ui";
import PlanClient from "./PlanClient";

export const metadata: Metadata = {
  title: "요금제",
  description: "문어CBT 자격증 학습 플랜 — Focus·Thunder·Pro·All Pass 가격 안내.",
  alternates: { canonical: "/plan" },
};

export default function PlanPage() {
  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="wide" className="py-20">
        <PlanClient />
      </Container>
    </main>
  );
}
