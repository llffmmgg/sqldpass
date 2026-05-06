import Spinner from "@/components/Spinner";

export default function WrongAnswersLoading() {
  return (
    <main className="flex min-h-[60vh] items-center justify-center">
      <Spinner message="오답 노트 불러오는 중..." />
    </main>
  );
}
