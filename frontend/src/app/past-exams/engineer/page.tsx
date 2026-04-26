import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const dynamic = "force-dynamic";
export const metadata = buildPastExamCertMetadata("engineer");

export default function EngineerPastExamsPage() {
  return <PastExamCertPage certSlug="engineer" />;
}
