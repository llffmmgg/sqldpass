import { redirect } from "next/navigation";

// /past-exams/{cert-slug} 라우트는 모두 /past-exams?cert=... 로 통합되었다.
// 외부 SEO/공유 링크 호환을 위해 기존 URL 은 살려두고 즉시 쿼리 방식으로 보낸다.
export default function SqldPastExamsRedirect(): never {
  redirect("/past-exams?cert=SQLD");
}
