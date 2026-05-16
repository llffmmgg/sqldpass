# Step 3 — 신규 MDX 글 + blog page 컴포넌트 매핑

## 배경

ADsP D-1 응원 + 정리본 안내 글을 작성하고, 블로그 라우트에서 `<AdspCramHero />` 가 MDX 안에서 렌더링되도록 `mdxComponents` 매핑에 추가한다. PDF 미리보기 1·2페이지 이미지는 사용자가 직접 `frontend/public/blog/` 에 둘 예정이므로 MDX 본문에서 정적 경로로 참조한다.

## 작업 디렉터리

```
frontend/
```

## 사용자 사전 작업 (코드 작업과 별개)

- `backend/src/main/resources/blog-downloads/adsp-cram.pdf` — 데스크탑 PDF 복사
- `frontend/public/blog/adsp-cram-preview-1.png`, `adsp-cram-preview-2.png` — PDF 1·2페이지 PNG 추출

이 파일들이 없어도 코드 자체는 빌드되며, 실제 동작 시점에만 필요.

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/content/blog/adsp-d1-cheer.mdx` | 신규 글 (frontmatter + `<AdspCramHero />` + 본문 + 미리보기 이미지 2개) |
| `frontend/src/app/blog/[slug]/page.tsx` | `mdxComponents` 객체에 `AdspCramHero` 추가 (1줄), import 1줄 |

## adsp-d1-cheer.mdx frontmatter

```yaml
---
title: "ADsP D-1, 마지막 정리본 (Thunder 회원 전용 PDF)"
description: "내일 ADsP 시험을 앞둔 Thunder 회원을 위한 마지막 정리본. 1·2페이지 미리보기와 전체 PDF 다운로드 안내."
date: "2026-05-16"
category: "ADsP"
tags: ["ADsP", "벼락치기", "정리본", "D-1"]
---
```

## 본문 (Claude 초안 — 200~300자, 톤은 사용자 메모리 blog-writing-style 준수: 결론→이유→팁, AI 클리셰 금지)

```mdx
<AdspCramHero />

## D-1, 새 개념을 더 외울 시간은 없습니다

내일이 ADsP 시험일이에요. 이 시점에 새 개념을 욱여넣기보다, **자주 나오는 함정과 1과목 점수를 거저 가져갈 개념을 한 번 더 훑는 게** 효율적입니다.

이번 정리본은 30분이면 한 번 더 돌릴 수 있도록 핵심만 압축했어요. 아래에서 1·2페이지를 미리 보고, **Thunder 회원** 분들은 전체 PDF 를 받아 가세요.

![ADsP 정리본 1페이지 미리보기](/blog/adsp-cram-preview-1.png)

![ADsP 정리본 2페이지 미리보기](/blog/adsp-cram-preview-2.png)

## 오늘 어떻게 쓰면 좋을까요

- **1과목(데이터 이해)** 은 무조건 만점 노리기 — DIKW, 빅데이터 정의, 데이터 사이언티스트 역량은 정리본 앞쪽 박스만 다시 보면 됩니다.
- **통계 파트** 는 공식 외우기보다 단위·정의를 다시 읽어요. 모비율·신뢰구간은 보기 비교만 빠르게.
- **시험장 직전** 한 번 더 — 새 내용은 추가하지 마세요. 컨디션이 점수입니다.

오늘 너무 늦게까지 붙잡지 말고, 푹 자고 내일 점수 잘 받아오세요. 화이팅!
```

## blog/[slug]/page.tsx 변경

L19 (`const mdxComponents = { PassRateBar, ... };`) 에 `AdspCramHero` 추가:

```ts
import AdspCramHero from "@/components/blog/AdspCramHero";
// ...
const mdxComponents = { PassRateBar, PassRateTrend, PassRateDial, StatBar, DistributionBar, Highlight, AdspCramHero };
```

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동:
1. `npm run dev` → `/blog/adsp-d1-cheer` 접속
2. 비로그인 → 히어로 박스에 "로그인하고 받기" 노출, 본문 미리보기 이미지 2개 표시 (이미지 파일이 배치되어 있을 때)
3. 로그인 + Free → "Thunder 플랜 보기" CTA 노출
4. 로그인 + Thunder → 다운로드 버튼 클릭 시 `adsp_문어_벼락치기.pdf` 다운로드 (PDF 배치되어 있을 때)
5. 다른 블로그 글(`/blog/adsp-cram-3days` 등) 정상 렌더링 확인

## Acceptance Criteria

1. `frontend/content/blog/adsp-d1-cheer.mdx` 가 생성되어 있고 frontmatter 가 정확하다.
2. `blog/[slug]/page.tsx` 의 `mdxComponents` 에 `AdspCramHero` 가 매핑되어 있다.
3. `npm run lint`, `npm run build` 통과 (MDX 컴파일 포함).
4. 다른 블로그 글 렌더링에 회귀가 없다.

## 금지 사항

- frontmatter 의 `date` 를 자동/임의 날짜로 바꾸지 마라. **이유**: 2026-05-16 (오늘) 으로 고정해야 D-1 글로서 의미가 있다.
- 다른 블로그 글의 본문이나 frontmatter 를 건드리지 마라. **이유**: 본 작업 범위 밖. 회귀 위험.
- 미리보기 이미지 파일을 임의로 생성하지 마라. **이유**: 사용자가 PDF 에서 직접 추출하기로 결정. Claude 가 placeholder 이미지를 만들면 안 됨.
- 본문에서 AI 클리셰("효율을 극대화", "압도적인 정리" 등) 를 쓰지 마라. **이유**: 사용자 메모리(blog-writing-style).

## Status 규칙

- 성공: step 3 `completed`, summary 에 "adsp-d1-cheer.mdx + mdxComponents 매핑, lint/build OK".
- 실패: 3회 재시도 후 `error`.
