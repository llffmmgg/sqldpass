import Spinner from "@/components/Spinner";

export default function SolveLoading() {
  return (
    <main className="flex min-h-[60vh] items-center justify-center">
      <Spinner message="문제 불러오는 중..." />
    </main>
  );
}
