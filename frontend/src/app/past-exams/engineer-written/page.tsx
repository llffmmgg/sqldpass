import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const metadata = buildPastExamCertMetadata("engineer-written");

export default function EngineerWrittenPastExamsPage() {
  return <PastExamCertPage certSlug="engineer-written" />;
}
