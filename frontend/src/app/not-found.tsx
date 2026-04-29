import Link from "next/link";

export const metadata = {
  title: "페이지를 찾을 수 없어요",
  robots: { index: false, follow: false },
};

export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center px-6 py-16">
      <div className="w-full max-w-md rounded-2xl border border-border bg-surface/60 p-8 text-center backdrop-blur">
        <div className="mb-4 text-5xl font-mono text-muted">404</div>
        <h1 className="mb-2 text-xl font-semibold text-foreground">
          페이지를 찾을 수 없어요
        </h1>
        <p className="mb-6 text-sm leading-relaxed text-muted">
          주소가 변경되었거나 더 이상 존재하지 않는 페이지일 수 있어요.
        </p>
        <div className="flex flex-col gap-2 sm:flex-row sm:justify-center">
          <Link
            href="/"
            className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition hover:bg-primary-hover"
          >
            홈으로
          </Link>
          <Link
            href="/learn"
            className="rounded-lg border border-border bg-transparent px-4 py-2 text-sm font-medium text-foreground transition hover:bg-surface"
          >
            기출문제 둘러보기
          </Link>
        </div>
      </div>
    </div>
  );
}
