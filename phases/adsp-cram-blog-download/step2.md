# Step 2 — Frontend: 다운로드 클라이언트 함수 + AdspCramHero 컴포넌트

## 배경

블로그 글 상단 히어로 박스에서 Thunder 이상 회원만 PDF 다운로드 버튼이 활성화되도록 한다. 비로그인/Free 등급은 안내 + CTA 만 표시. 백엔드 검증이 최종 가드이므로 클라이언트 분기는 UX 용이다.

기존 `downloadMockExamPdfAsUser` (`frontend/src/lib/payment.ts` L165) 의 fetch + blob 트리거 패턴, `MockExamPdfButton` 의 토스트 처리, `useSubscription` 훅을 그대로 재사용한다.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/lib/payment.ts` | `downloadAdspCramPdf()` 함수 추가 (기존 `PdfDownloadError` 재사용) |
| `frontend/src/components/blog/AdspCramHero.tsx` | 신규 클라이언트 컴포넌트 |

## payment.ts 변경

`downloadMockExamPdfAsUser` 바로 아래에 추가:

```ts
/**
 * ADsP D-1 정리본 PDF 다운로드 — Thunder 이상 회원 전용 (백엔드 검증).
 */
export async function downloadAdspCramPdf(): Promise<void> {
  const token = getToken();
  const res = await fetch("/api/blog-downloads/adsp-cram", {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    let code = `HTTP_${res.status}`;
    let message = `PDF 다운로드 실패 (${res.status})`;
    try {
      const body = await res.json();
      if (body?.code) code = body.code;
      if (body?.message) message = body.message;
    } catch {
      /* body 파싱 실패 시 기본 메시지 사용 */
    }
    throw new PdfDownloadError(code, message);
  }
  const disposition = res.headers.get("Content-Disposition") ?? "";
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  const asciiMatch = /filename="?([^";]+)"?/i.exec(disposition);
  const filename = utf8Match
    ? decodeURIComponent(utf8Match[1])
    : asciiMatch?.[1] ?? "adsp-cram.pdf";

  const blob = await res.blob();
  const objectUrl = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = objectUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(objectUrl), 100);
}
```

## AdspCramHero 컴포넌트

- `"use client"`
- 의존: `useSubscription`, `isLoggedIn`, `useToast`, `downloadAdspCramPdf`, `PdfDownloadError`, `Link` (from "next/link")
- 렌더:
  - 박스: Supabase 톤 — `rounded-2xl border border-border bg-surface p-6` (backdrop-blur·glow 금지)
  - 상단 작은 라벨 "Thunder 회원 전용", 큰 제목 "ADsP 마지막 정리본 PDF", 짧은 설명
  - CTA 분기 (loading 상태는 disabled 처리):
    - `loading=true` → 회색 비활성 버튼 "확인 중..."
    - 비로그인 (`isLoggedIn() === false`) → Link href="/login" 버튼 "로그인하고 받기"
    - 로그인 + `subscription.allowsPdf === false` → Link href="/plan" 버튼 "Thunder 플랜 보기" + 보조 텍스트
    - 로그인 + `subscription.allowsPdf === true` → 클릭 시 `downloadAdspCramPdf()` 호출. 에러 시 `PDF_REQUIRES_SUBSCRIPTION` 토스트(`MockExamPdfButton` 문구와 동일 톤) 또는 일반 에러 토스트
- 기존 패턴 그대로 — `MockExamPdfButton` L21–45 의 try/catch + toast 분기 거의 복붙

## 검증

```powershell
cd frontend
npm run lint
```

(`npm run build` 는 step3 에서 MDX 글까지 함께 묶어서 한 번에 검증)

## Acceptance Criteria

1. `downloadAdspCramPdf` 가 `payment.ts` 에 추가되어 있고, 기존 `PdfDownloadError` 를 재사용한다.
2. `AdspCramHero` 컴포넌트가 신규 추가되어 있고, 등급별 3분기(비로그인/Free/Thunder+)가 구현되어 있다.
3. `npm run lint` 통과.

## 금지 사항

- 클라이언트에서 등급 확인을 우회 가능한 방식으로 "PDF URL 노출" 하지 마라. **이유**: 본 작업의 핵심은 백엔드 게이팅. 클라이언트 분기는 UX 용으로만.
- 새 토스트 문구를 만들지 마라. **이유**: 기존 `MockExamPdfButton` 의 `PDF_REQUIRES_SUBSCRIPTION` 토스트 문구와 같은 톤을 유지해 일관성 확보.
- `backdrop-blur`, `drop-shadow` glow, opacity pulse, `/5~/10` 옅은 배경을 쓰지 마라. **이유**: 사용자 메모리(no-ai-blur-effects) — Supabase 단단한 톤 유지.
- 색 계열을 바꾸지 마라. **이유**: 사용자 메모리(color-token-changes) — 기존 토큰만 사용, shade/opacity 만 조정.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "downloadAdspCramPdf + AdspCramHero 추가, lint OK".
- 실패: 3회 재시도 후 `error`.
