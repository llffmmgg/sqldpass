import { notFound } from "next/navigation";

import PastExamRunnerClient from "@/components/past-exams/PastExamRunnerClient";
import { getPublicPastExam } from "@/lib/publicApi";

export default async function PastExamDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const examId = Number(id);

  if (!Number.isInteger(examId)) {
    notFound();
  }

  const exam = await getPublicPastExam(examId).catch(() => null);
  if (!exam) {
    notFound();
  }

  return <PastExamRunnerClient initialExam={exam} />;
}
