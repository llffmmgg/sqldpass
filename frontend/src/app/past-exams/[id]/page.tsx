import { notFound } from "next/navigation";

import PastExamRunnerClient from "@/components/past-exams/PastExamRunnerClient";
import { getPublicPastExam } from "@/lib/publicApi";
import { CERT_DISPLAY, buildRoundTitle } from "@/lib/pastExamRoundTitle";

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

  const certLabel = CERT_DISPLAY[exam.examType] ?? exam.name;
  const roundTitle = buildRoundTitle(exam);

  return (
    <>
      {/* SEO 전용 — 화면 비표시. 회차 풀이 UI 가 클라이언트 컴포넌트라
          첫 HTML 안에 텍스트 콘텐츠가 거의 없는 문제를 보정한다. */}
      <div className="sr-only">
        <h1>{roundTitle} 기출 복원</h1>
        <p>
          {roundTitle} 기출 복원 모의고사 — 총 {exam.totalQuestions}문항. {certLabel} 정기 회차의
          기출을 복원해 실전 시간 제한과 동일한 타이머로 풀어볼 수 있고, 로그인 후 자동 채점과
          해설까지 이어서 확인할 수 있습니다.
        </p>
      </div>
      <PastExamRunnerClient initialExam={exam} />
    </>
  );
}
