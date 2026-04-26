import PastExamCertPage from "@/components/past-exams/PastExamCertPage";
import { buildPastExamCertMetadata } from "@/lib/pastExamSeo";

export const dynamic = "force-dynamic";
export const metadata = buildPastExamCertMetadata("computer-literacy-2");

export default function ComputerLiteracyTwoPastExamsPage() {
  return <PastExamCertPage certSlug="computer-literacy-2" />;
}
