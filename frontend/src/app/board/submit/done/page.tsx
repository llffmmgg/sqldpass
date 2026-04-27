import Link from "next/link";
import { Container } from "@/components/ui";

export default function SubmitDonePage() {
  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-20 text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-3xl">
          ✅
        </div>
        <h1 className="mt-5 text-2xl font-bold tracking-tight sm:text-3xl">
          후기 제출이 완료되었어요
        </h1>
        <p className="mt-3 text-sm text-text-muted">
          운영자 검토 후 1~2일 이내에 게시판에 노출됩니다.
          <br />
          소중한 후기 감사합니다 🙏
        </p>
        <div className="mt-8 flex justify-center gap-2">
          <Link
            href="/board"
            className="inline-flex items-center rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-primary-fg hover:bg-primary-hover"
          >
            게시판으로
          </Link>
          <Link
            href="/dashboard"
            className="inline-flex items-center rounded-md border border-border bg-surface px-5 py-2.5 text-sm text-text-muted hover:border-border-strong hover:text-text"
          >
            대시보드로
          </Link>
        </div>
      </Container>
    </main>
  );
}
