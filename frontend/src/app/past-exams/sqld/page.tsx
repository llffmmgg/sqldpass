import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const dynamic = "force-dynamic";
export const metadata = buildPastExamCertMetadata("sqld");

export default function SqldPastExamsPage() {
  return <PastExamCertPage certSlug="sqld" />;
}
