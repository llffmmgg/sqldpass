import Spinner from "@/components/Spinner";

export default function PastExamsLoading() {
  return (
    <main className="flex min-h-[60vh] items-center justify-center">
      <Spinner message="기출 불러오는 중..." />
    </main>
  );
}
