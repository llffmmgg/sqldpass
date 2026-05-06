import Spinner from "@/components/Spinner";

export default function MockExamDetailLoading() {
  return (
    <main className="flex min-h-[60vh] items-center justify-center">
      <Spinner message="모의고사 불러오는 중..." />
    </main>
  );
}
