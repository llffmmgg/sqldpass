import { redirect } from "next/navigation";

// /plan 은 과거 가격 카탈로그 페이지였으나, /checkout 이 동일한 카드 UI 와 결제 흐름을
// 모두 갖고 있어 사용자에게 같은 카드를 두 번 보여주는 중복 흐름이었다. 라우트는 유지하되
// 외부 SEO/공유 링크가 들어오면 즉시 /checkout 으로 보낸다.
export default function PlanPage(): never {
  redirect("/checkout");
}
