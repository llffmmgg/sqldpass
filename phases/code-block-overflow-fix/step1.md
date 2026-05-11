# Step 1 — 코드 블록 가로 overflow 가로 스크롤로 처리

## 배경

사용자 피드백 #61(문제 #5371), #62(문제 #5373) — `/mock-exams/112` 에서 본문 코드 블록이 좁은 뷰포트의 컨테이너 영역을 벗어나 잘림.

원인: `frontend/src/components/QuestionCodeBlock.tsx:64-77` 의 `SyntaxHighlighter` 의 `customStyle` 에 `overflowX` 와 `whiteSpace` 가 명시되지 않아, 좁은 뷰포트에서 내부 토큰 span 들이 inline 으로 가로 확장 → 외곽 `rounded-lg overflow-hidden` wrapper 에 의해 잘림.

수정 정책: **가로 스크롤** (`overflow-x: auto` + `white-space: pre`). 코드 인덴트/구조 보존 유리, 콘텐츠가 폭 안에 들어가는 짧은 코드의 시각은 변화 없음.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

수정 1개:

| 파일 | 변경 |
|------|------|
| `frontend/src/components/QuestionCodeBlock.tsx` (line 68-74) | `customStyle` 객체에 `overflowX: "auto"` 와 `whiteSpace: "pre"` 두 줄 추가 |

## 변경 내용 (정확한 diff)

```tsx
// before (line 68-74)
customStyle={{
  margin: 0,
  padding: "1rem",
  background: "transparent",
  fontSize: "0.875rem",
  lineHeight: 1.6,
}}

// after
customStyle={{
  margin: 0,
  padding: "1rem",
  background: "transparent",
  fontSize: "0.875rem",
  lineHeight: 1.6,
  overflowX: "auto",
  whiteSpace: "pre",
}}
```

코드 동작/시그니처/타입 변경 없음. 인라인 style 두 줄 추가뿐.

## 회귀 분석 — 기존 잘 보이던 코드는?

| 케이스 | 변화 |
|--------|------|
| 짧은 코드(한 줄 폭 내 포함) | **시각 변화 0**. `overflow-x: auto` 는 넘칠 때만 스크롤바, `white-space: pre` 는 syntax-highlighter 가 이미 사실상 pre 처럼 렌더링 |
| 여러 줄 짧은 코드 | 시각 변화 0 |
| 데스크탑 긴 코드 | 가로 스크롤바로 깔끔하게 처리 |
| 모바일 긴 코드(신고 케이스) | **가로 스크롤로 정상 표시** — 본 수정의 목적 |
| 본문 markdown 단락/리스트/표 | 영향 0 — 본 컴포넌트는 코드 블록 전용 |
| header `● ● ● Java` 정렬 | 영향 0 — header 는 별도 div, customStyle 은 코드 영역에만 적용 |
| 둥근 모서리 | 유지 — 외곽 wrapper `overflow-hidden` 그대로 |
| 선택지/해설 코드 (같은 컴포넌트 재사용) | 본문과 동일하게 보정 |

핵심: **짧은 코드의 시각 표시는 그대로**, 콘텐츠가 폭을 넘을 때만 스크롤바가 생긴다.

## 컴포넌트 재사용 위치 (한 곳 수정으로 전체 보정)

- `frontend/src/app/mock-exams/[id]/page.tsx:591` 본문 + `:770` 선택지
- `frontend/src/app/history/[id]/page.tsx:176, 209, 225` 본문/선택지/해설
- `frontend/src/components/DailyQuestionWidget.tsx` 일일 문제

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

위 둘 통과만으로 자동 검증 완료. 변경이 인라인 style 두 줄이라 컴파일·타입·번들 영향 없음.

### (옵션) 시각 회귀 체크리스트

`npm run dev` 로 dev 서버 띄우고 `/mock-exams/112` 진입 시:

- [ ] 짧은 코드 블록(한 줄) — header 와 padding 정렬, 시각 표시 그대로
- [ ] 신고된 긴 코드(Java interface) — 가로 스크롤바로 끝까지 보임
- [ ] 모바일 viewport(375px) — 페이지 전체 가로 스크롤이 새지 않고 코드 영역 안에서만 스크롤
- [ ] header(`● ● ● Java`) 와 코드 padding 정렬 유지

이 체크리스트는 사용자가 머지 전 수동 확인. 본 step 의 AC 통과 기준은 lint + build.

## Acceptance Criteria

1. `frontend/src/components/QuestionCodeBlock.tsx` 의 line 68-74 `customStyle` 객체에 `overflowX: "auto"` 와 `whiteSpace: "pre"` 두 줄이 추가된다.
2. 다른 라인/파일 변경 0건 (인라인 style 두 줄만).
3. `npm run lint` 결과 errors 0, warning 회귀 없음.
4. `npm run build` ✓ Compiled successfully.
5. `customStyle` 객체의 키 순서 유지(margin, padding, background, fontSize, lineHeight, overflowX, whiteSpace).

## 금지 사항

- 외곽 wrapper(line 57) 의 `overflow-hidden` 을 `overflow-x-auto` 로 바꾸지 마라. 이유: 둥근 모서리(rounded-lg) 가 스크롤바와 함께 잘려 시각적으로 어색해진다 — 내부 SyntaxHighlighter customStyle 에서 처리하는 게 정확.
- `whiteSpace: "pre-wrap"` 또는 `wordBreak` 추가 금지. 이유: 사용자 결정은 가로 스크롤 — 줄바꿈은 인덴트 손상이라 정책에 반함.
- 다른 컴포넌트(QuestionContent 등) 를 수정하지 마라. 이유: 본 step 은 QuestionCodeBlock 한 곳 수정으로 충분 — 변경 범위 최소화.
- globals.css 또는 Tailwind config 를 수정하지 마라. 이유: 인라인 customStyle 으로 충분, CSS 글로벌 영향 회피.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "QuestionCodeBlock customStyle 에 overflow-x auto + white-space pre 추가, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: lint/build 가 통과 못 할 경우 (인라인 style 두 줄 추가라 회귀 가능성 낮음).
