import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const revalidate = 1800;
export const metadata = buildPastExamCertMetadata("adsp");

export default function AdspPastExamsPage() {
  return <PastExamCertPage certSlug="adsp" />;
}
