import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const metadata = buildPastExamCertMetadata("computer-literacy-1");

export default function ComputerLiteracyOnePastExamsPage() {
  return <PastExamCertPage certSlug="computer-literacy-1" />;
}
