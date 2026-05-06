import Spinner from "@/components/Spinner";

export default function DashboardLoading() {
  return (
    <main className="flex min-h-[60vh] items-center justify-center">
      <Spinner message="대시보드 불러오는 중..." />
    </main>
  );
}
