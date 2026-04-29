import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const revalidate = 1800;
export const metadata = buildPastExamCertMetadata("engineer");

export default function EngineerPastExamsPage() {
  return <PastExamCertPage certSlug="engineer" />;
}
