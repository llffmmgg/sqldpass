import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const revalidate = 1800;
export const metadata = buildPastExamCertMetadata("computer-literacy-1");

export default function ComputerLiteracyOnePastExamsPage() {
  return <PastExamCertPage certSlug="computer-literacy-1" />;
}
