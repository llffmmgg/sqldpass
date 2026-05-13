# Step 3 — `/history/{id}` 상세 페이지 2단계 접힘 구조

## 배경

step 2 에서 기출복원 결과 화면의 인라인 아코디언이 로그인 사용자에서 제거되고 "상세 보기" 가 `/history/{solveId}` 로 일원화된다. 모의고사 "상세 보기" 도 같은 페이지로 향한다.

현재 `/history/{id}` 페이지(`frontend/src/app/history/[id]/page.tsx:119-235`) 는 문제 본문/선택지를 모두 펼친 채 줄줄이 나열하고, **해설만** `<details>` 로 접어둔 절반짜리 접힘이다. 30~80문항이 한꺼번에 노출되어 스캔이 어렵다.

사용자 요청:
- **1단계 (카드)**: 카드가 닫힌 채로 시작. 클릭하면 문제 본문 + 선택지(정답/내 답 강조 포함) 가 펼쳐진다.
- **2단계 (해설)**: 펼친 카드 안에서 "해설 보기" 를 또 한 번 클릭해야 해설이 노출된다 (현재 `<details>` 패턴 그대로 유지).

기존 1단계 아코디언 패턴은 `frontend/src/components/past-exams/PastExamRunnerClient.tsx:957-1080` 의 `PastExamReviewCard` 가 이미 동일 구조다. 단 그 카드는 해설을 펼친 영역에 그대로 노출하므로, history 페이지에서는 해설 블록만 `<details>` 로 추가 감싸 2단계로 만든다.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/app/history/[id]/page.tsx` | 158~232줄의 문제 카드 리스트를 2단계 접힘 아코디언으로 교체. `openIdx` state 추가. 점수 카드/광고/회차 라벨/대시보드 링크는 그대로. |

## 구현

### A. state 추가

`HistoryDetailContent` 함수 본문 상단(52~57줄 state 블록 근처)에 추가:

```ts
const [openIdx, setOpenIdx] = useState<number | null>(null);
```

### B. 문제 카드 리스트(158~232줄) 교체

각 카드 컴포넌트 구조:

**닫힌 카드 (헤더 버튼)**:
- 좌측: 정오 배지(`✓` 초록 / `✗` 빨강 / `–` 미응답 회색) — `PastExamReviewCard:984-993` 패턴 차용
- "문제 N" 라벨 (현재 174줄과 동일)
- 우측 끝: `▾` 토글 아이콘 (열림 시 `rotate-180`)
- 카드 자체는 `rounded-lg border` (정답 카드 `border-green-500/30 bg-green-500/5`, 오답 카드 `border-red-500/30 bg-red-500/5`, 미응답 `border-border bg-bg`)

**열린 카드 (펼친 본문)**:
- 문제 본문: `<QuestionContent content={parsed.body} />` (현재 175~177줄 그대로)
- 선택지 리스트(MCQ): 현재 190~217줄의 정답/내 답 강조 로직 그대로 유지
- 단답/서술형: 현재 코드엔 없지만 history 페이지가 MCQ 가정이므로 그대로 둠. 만약 `parsed.options.length === 0` 이면 "내 답" 텍스트 표시(detail.answer/keywords 가 있으면 함께)
- 해설 블록: **2단계 접힘** — 현재 219~228줄의 `<details>...<summary>해설 보기</summary>` 패턴 **그대로 유지**

핵심 차이는 카드 컨테이너가 `<button>` 헤더 + `{open && <펼친 영역>}` 으로 1단계 토글이 추가되는 것뿐.

권장 마크업 스켈레톤(헤더만):

```tsx
const answered = answer.selectedOption != null;
const status: "correct" | "wrong" | "unanswered" =
  !answered ? "unanswered" : answer.correct ? "correct" : "wrong";

<div
  key={answer.questionId}
  className={`rounded-lg border overflow-hidden ${
    status === "correct"
      ? "border-green-500/30 bg-green-500/5"
      : status === "wrong"
      ? "border-red-500/30 bg-red-500/5"
      : "border-border bg-bg"
  }`}
>
  <button
    onClick={() => setOpenIdx(openIdx === idx ? null : idx)}
    className="flex w-full items-center gap-3 px-5 py-4 text-left transition-colors hover:bg-surface-hover"
  >
    <span className={`inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
      status === "correct" ? "bg-success/15 text-success"
      : status === "wrong" ? "bg-danger/15 text-danger"
      : "bg-surface-hover text-text-subtle"
    }`}>
      {status === "correct" ? "✓" : status === "wrong" ? "✗" : "–"}
    </span>
    <span className="flex-1 text-sm font-medium text-text">문제 {idx + 1}</span>
    <span className={`text-xs text-text-subtle transition-transform ${openIdx === idx ? "rotate-180" : ""}`}>▾</span>
  </button>

  {openIdx === idx && (
    <div className="border-t border-border px-5 py-4">
      {parsed && <QuestionContent content={parsed.body} />}

      {parsed && parsed.options.length > 0 && (
        <div className="mt-3 space-y-1">
          {/* 기존 192~217줄의 선택지 매핑 로직 그대로 */}
        </div>
      )}

      {detail && detail.explanation && (
        <details className="mt-3 rounded-lg border border-border px-3 py-2 text-sm">
          <summary className="cursor-pointer font-medium text-amber-400">해설 보기</summary>
          <div className="mt-2 leading-relaxed text-muted">
            <QuestionContent content={detail.explanation} />
          </div>
        </details>
      )}
    </div>
  )}
</div>
```

선택지 매핑 로직(192~217줄)은 그대로 복붙. 정답/내 답 강조 색상(`bg-green-500/10 text-green-400`, `bg-red-500/10 text-red-400`) 도 변경하지 않는다.

### C. 점수 카드/광고/대시보드 링크 그대로

119~155줄(헤더, 점수 카드, 광고)은 수정하지 않는다.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 검증(`npm run dev`):

1. **로그인 후 모의고사 풀이 완료** → 결과 화면 "상세 보기" 클릭 → `/history/{id}` 진입
   - 모든 문제 카드가 **닫힌 상태**로 시작 (정오 배지 + "문제 N" 만 노출)
   - 첫 번째 카드 클릭 → 본문 + 선택지가 펼쳐지고, "해설 보기" `<details>` 가 **닫힌 상태**로 등장
   - "해설 보기" 한 번 더 클릭 → 해설 노출
   - 두 번째 카드 클릭 → 첫 카드 닫히고 두 번째 카드 펼침 (`openIdx` 단일 토글)
2. **기출복원 풀이 완료** → 동일 페이지에서 동일 동작
3. **미응답 문제** → `–` 회색 배지 + 펼치면 선택지에 "선택" 표시 없음
4. **데스크탑/모바일** — 카드 너비, 토글 동작, 텍스트 줄바꿈 정상

## Acceptance Criteria

1. `history/[id]/page.tsx` 에 `openIdx` state 가 추가되었고 `useState<number | null>(null)` 로 초기화된다.
2. 페이지 진입 직후 모든 문제 카드가 닫힌 상태로 시작한다(`openIdx === null`).
3. 카드 헤더 버튼을 클릭하면 해당 인덱스가 토글되어 본문/선택지가 노출되고, 같은 카드를 다시 클릭하면 닫힌다.
4. 다른 카드 클릭 시 이전 카드는 자동으로 닫힌다 (단일 `openIdx` 패턴).
5. 펼친 카드 안의 해설 블록은 여전히 `<details><summary>해설 보기</summary>` 형태로 **닫힌 채** 등장한다.
6. 정답/오답/미응답 시각 구분(`✓/✗/–` 배지 + 카드 보더 색) 이 일관되게 적용된다.
7. 기존 선택지 정답/내 답 강조 로직(192~217줄) 이 그대로 보존된다.
8. 점수 카드(127~147줄), 광고(149~155줄), 대시보드 링크(123~125줄) 가 변경되지 않았다.
9. `npm run lint` errors 0 (기존 warning 외 신규 회귀 없음).
10. `npm run build` ✓ Compiled successfully.

## 금지 사항

- 해설을 펼친 카드와 함께 자동으로 노출시키지 마라. 이유: 사용자가 "해설은 한 번 더 접혀있어야 한다" 고 명시. `<details>` 의 기본 닫힘 상태를 유지해야 한다.
- 카드 펼침 패턴을 `<details>` 로 일원화하지 마라. 이유: `<details>` 는 다중 펼침 허용이라 사용자가 명시한 "다른 카드 클릭 시 이전 카드 닫힘" (단일 `openIdx`) 패턴을 만족할 수 없다. 1단계는 `useState` 기반, 2단계는 `<details>` 로 분리한다.
- `getSolve` / `getQuestionDetail` 호출 흐름(62~94줄) 을 변경하지 마라. 이유: 데이터 페칭은 현행 그대로 두고 렌더 패턴만 바꾸는 것이 본 step 의 범위.
- `OPTION_MARKERS` 또는 `parseQuestion` 의 사용 방식을 바꾸지 마라. 이유: 표시 일관성 유지 — 다른 페이지(blog SEO, 풀이 화면 등)가 동일 마커 시스템에 의존한다.
- 카드 정답/오답 보더 색(`border-green-500/30`, `border-red-500/30`) 을 시맨틱 토큰으로 일제 교체하지 마라. 이유: 본 step 범위는 동작 패턴(2단계 접힘) 변경. 색상 토큰 마이그레이션은 별도 디자인 정리 작업.
- `view_result` GA4 이벤트(66~72줄) 를 옮기거나 페이로드를 바꾸지 마라. 이유: 추적 일관성. 페이지 진입 시 1회 발생이 정상 동작.

## Status 규칙

- 성공: step 3 `completed`, summary 에 "history 페이지 2단계 접힘 적용 — 카드 토글 + 해설 details, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: `useState` 추가가 React 19 의 `set-state-in-effect` 룰과 충돌하면 — 본 변경은 effect 가 아닌 사용자 이벤트(onClick)에서 호출하므로 충돌 없음. 만약 다른 ESLint 규칙과 충돌 시 사용자 확인.
