# Step 2 — 기출복원 결과 화면을 모의고사 frame 으로 통일

## 배경

`/mock-exams/[id]` 결과 화면(`frontend/src/app/mock-exams/[id]/page.tsx:289-437`)은 마스코트 + 격려 멘트 + PDF + 광고 + GA4 `complete_exam` + 마일스톤 토스트 + 합격 배너 + 과목별 점수 + "상세 보기" 버튼으로 구성된다. 반면 `/past-exams/[id]` 결과 화면(`PastExamRunnerClient.tsx:490-636` 의 `PastExamResultView`)은 점수/합격배너/과목별 점수 + **인라인 아코디언 PastExamReviewList** + "상세 보기" 만 있고 마스코트/격려/PDF/광고/GA4/마일스톤이 모두 빠져있다.

사용자 결정:
- 양쪽 결과 화면 frame 은 **모의고사 형식으로 통일**.
- 결과 화면의 **인라인 아코디언은 로그인 사용자에서 제거** → "상세 보기" 버튼으로 `/history/{id}` 유도(step 3 에서 history 페이지를 2단계 접힘으로 통일).
- 비로그인은 `solveId === null` 이라 `/history/{id}`(AuthGuard) 진입 불가 → **인라인 PastExamReviewList fallback 유지 + 로그인 유도 CTA**.
- PDF·광고·GA4·마일스톤 토스트 전부 기출복원에도 적용.

이 step 은 step 1(백엔드 `milestoneReached` 필드 추가) 직후 실행한다.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/lib/pastExamApi.ts` | `PastExamGradeResponse` 인터페이스에 `milestoneReached?: number \| null` 추가 |
| `frontend/src/components/past-exams/PastExamRunnerClient.tsx` | `PastExamResultView` 를 모의고사 frame 으로 재구성. `handleSubmit` 에 `complete_exam` 트래킹 + 마일스톤 토스트 |

## 구현

### A. `pastExamApi.ts` — 타입 보강

`PastExamGradeResponse` (77~90줄) 마지막에 필드 추가:

```ts
export interface PastExamGradeResponse {
  totalCount: number;
  correctCount: number;
  score: number;
  items: PastExamGradedItem[];
  solveId: number | null;
  subjectScores: PastExamSubjectScore[];
  passed: boolean;
  passReason: string;
  /** 학습 연속일 마일스톤 도달 일수 — 도달 안 했으면 null, 비로그인이면 null */
  milestoneReached?: number | null;
}
```

### B. `PastExamRunnerClient.tsx` — import 추가

파일 상단 import 블록에 추가:

```ts
import MockExamPdfButton from "@/components/MockExamPdfButton";
import MascotImage from "@/components/mascot/MascotImage";
import { poseFromScore } from "@/components/mascot";
import AdInfeed from "@/components/AdInfeed";
import AdDisplay from "@/components/AdDisplay";
import { trackEvent } from "@/lib/gtag";
import { useToast } from "@/components/Toast";
```

### C. `handleSubmit` 보강 (266~302줄)

`gradePastExam` 호출 직후 `hapticSuccess/hapticError` 분기 다음에 다음 두 가지 추가:

1. **마일스톤 토스트** — 로그인 + 마일스톤 도달 시:
   ```ts
   if (graded.milestoneReached) {
     toast.show(`🎉 ${graded.milestoneReached}일 연속 학습! 잘하고 있어요`, "success");
   }
   ```

2. **GA4 complete_exam** — 모의고사와 동일 페이로드(`mock-exams/[id]/page.tsx:505-512`):
   ```ts
   trackEvent("complete_exam", {
     exam_id: exam.id,
     exam_type: exam.examType,
     score: graded.score,
     correct_count: graded.correctCount,
     total_count: graded.totalCount,
   });
   ```

`toast` 는 `PastExamRunnerClient` 컴포넌트 본문 상단에서 `const toast = useToast();` 로 확보.

### D. `PastExamResultView` 재구성 (490~636줄)

기존 구조를 다음 순서로 교체. `PastExamResultView` 는 props 에 `exam`, `result`, `onRestart` 만 받으므로 시그니처는 그대로 유지하고 내부 JSX 만 변경.

레이아웃(위→아래):

1. **상단 헤더 라인**
   - 좌측: `Badge cert={cert} variant="soft" size="sm" dot` + 회차 라벨(`roundLabel`) 작은 텍스트
   - 우측: `<MockExamPdfButton examId={exam.id} />`
   - 제목: `<h1>{exam.name} 결과</h1>` (모의고사 page.tsx:312 와 같은 톤, `text-2xl font-bold mt-3`)

2. **합격/불합격 배너** — 기존 526~545줄 그대로 유지. (서버 `result.passed/passReason` 사용)

3. **마스코트 점수 카드** — 모의고사 page.tsx:340-365 와 같은 구조:
   ```tsx
   <div className="mt-6 rounded-xl border border-border bg-surface p-6 sm:p-8">
     <div className="flex items-center gap-4 sm:gap-6">
       <MascotImage pose={poseFromScore(result.score)} size={120} className="shrink-0" />
       <div className="min-w-0 flex-1 text-left">
         <p className="text-sm text-text-muted">점수</p>
         <p className={`mt-1 text-5xl font-bold tabular-nums leading-none animate-score-pop ${token.tailwind.text}`}>
           {result.score}
           <span className="ml-1 align-middle text-2xl text-text-subtle">/100</span>
         </p>
         <p className="mt-3 text-sm text-text-muted">
           {result.correctCount} / {result.totalCount} 정답 · <span className={rateTone}>{correctRate}%</span>
         </p>
         <p className="mt-3 text-sm leading-relaxed text-text-muted">
           {result.score >= 90
             ? "거의 다 맞히셨어요. 남은 한두 문항만 정리해볼까요?"
             : result.score >= 60
             ? "합격선을 넘었어요. 이 페이스를 유지해볼까요?"
             : "한 발자국 더 가볼까요? 오답을 함께 정리해보세요."}
         </p>
       </div>
     </div>
   </div>
   ```
   - `token.tailwind.text` 는 자격증 액센트 텍스트 클래스(기존 `CERT_TOKENS` 활용). 적절한 키가 없으면 amber 기본값 → `text-amber-300` 로 대체.
   - 정답률(`correctRate`)/`rateTone` 은 기존 코드(503~513줄) 그대로 재사용.

4. **과목별 점수 표** — 기존 563~597줄 그대로 유지.

5. **광고 자리** — 모의고사 page.tsx:403-411 와 같은 위치(점수/과목 다음, 액션 버튼 직전):
   ```tsx
   <div className="mt-6 md:hidden">
     <AdInfeed adSlot="5227022543" adLayoutKey="-h4-h+1c-4h+8p" />
   </div>
   <div className="mt-6 hidden md:block">
     <AdDisplay adSlot="3622084801" />
   </div>
   ```

6. **인라인 아코디언 분기 노출**:
   - **로그인 + `result.solveId != null`**: `PastExamReviewList` 호출 **제거**. `/history/{solveId}` 로 유도.
   - **비로그인 (`result.solveId == null`)**: 기존 `<PastExamReviewList exam={exam} items={result.items} />` 유지. 위/아래에 로그인 유도 안내 박스 1개 추가:
     ```tsx
     <div className="mt-8 rounded-lg border border-primary/20 bg-primary/[0.04] p-4 text-sm">
       <p className="text-text">
         <Link href="/login" className="font-semibold text-primary underline">로그인</Link> 하면 풀이 기록이 자동 저장되고, "상세 보기" 페이지에서 같은 채점 결과를 언제든 다시 볼 수 있어요.
       </p>
     </div>
     ```

7. **하단 액션 버튼** — 기존 601~622줄 분기 유지하되 라벨 정렬:
   - 좌: `"다시 응시"` (`onRestart`)
   - 우: `result.solveId != null` 이면 `"상세 보기"` → `/history/{result.solveId}`, 아니면 `"다른 회차 보기"` → `backHref`

8. **하단 회차 목록 링크** — 기존 624~631줄 유지.

### E. `MockExamAttemptsView` 인터스티셜 화면(200~218줄)

여기엔 변경 없음. 사용자가 "결과 화면" 만 통일 요청.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 검증(개발 서버 `npm run dev`):

1. **로그인 + 기출복원 1회차 풀고 제출**
   - 결과 화면에 마스코트 + 격려 멘트(점수별 분기) 노출
   - PDF 버튼이 모의고사와 동일 위치(우상단)에 노출
   - 광고가 점수/과목 표 다음, 액션 버튼 직전에 노출
   - 인라인 `PastExamReviewList` 가 **렌더되지 않음**
   - "상세 보기" 클릭 시 `/history/{solveId}` 진입
   - GA4 DebugView 에서 `complete_exam` 이벤트 페이로드(exam_id, exam_type, score, correct_count, total_count) 확인
   - 마일스톤 일수에 도달했을 때(예: 7일차) 토스트 "🎉 N일 연속 학습! 잘하고 있어요" 노출
2. **비로그인 + 기출복원 풀고 제출**
   - "상세 보기" 버튼 대신 "다른 회차 보기" 버튼 노출
   - 인라인 `PastExamReviewList` 정상 노출
   - 위 또는 아래에 "로그인 하면 풀이 기록이 자동 저장" 안내 박스 표시
3. **모바일/데스크탑 반응형** — 마스코트 카드/광고가 양쪽에서 깨지지 않는지 확인

## Acceptance Criteria

1. `pastExamApi.ts` 의 `PastExamGradeResponse` 에 `milestoneReached?: number | null` 필드가 마지막에 추가되었다.
2. `PastExamRunnerClient.tsx` 에서 `MockExamPdfButton`, `MascotImage`, `poseFromScore`, `AdInfeed`, `AdDisplay`, `trackEvent`, `useToast` 가 import 되어 있다.
3. `handleSubmit` 안에 `trackEvent("complete_exam", { ... })` 호출이 있고 페이로드 키가 모의고사와 동일(`exam_id`, `exam_type`, `score`, `correct_count`, `total_count`).
4. `handleSubmit` 안에 `graded.milestoneReached` 가 truthy 일 때만 토스트를 띄우는 분기가 있다.
5. `PastExamResultView` 안에 `MascotImage`, `MockExamPdfButton`, `AdInfeed`, `AdDisplay` 가 렌더된다.
6. 로그인 사용자(`result.solveId != null`)일 때 `PastExamReviewList` 가 렌더되지 않는다.
7. 비로그인(`result.solveId == null`) 일 때 `PastExamReviewList` 와 로그인 유도 안내가 함께 렌더된다.
8. `npm run lint` errors 0 (기존 warning 외 신규 회귀 없음).
9. `npm run build` ✓ Compiled successfully.
10. `MockExamAttemptsView` 인터스티셜(200~218줄), `gradePastExam` 페이로드 빌드 로직(278~287줄), `PastExamReviewList`/`PastExamReviewCard` 구현은 변경하지 않는다.

## 금지 사항

- `PastExamReviewList` / `PastExamReviewCard` 컴포넌트를 삭제하지 마라. 이유: 비로그인 fallback 으로 그대로 유지해야 한다.
- 모의고사 결과 화면(`mock-exams/[id]/page.tsx`) 을 수정하지 마라. 이유: 본 step 범위는 기출복원 결과 화면을 모의고사 frame 으로 맞추는 것. 모의고사 자체는 이미 목표 형태.
- `handleSubmit` 의 grade 호출/페이로드 빌드(278~287줄) 와 `hapticSuccess/hapticError` 분기(291~296줄) 를 변경하지 마라. 이유: 채점 입력/응답 매핑은 step 1 백엔드 변경 외에는 그대로 유지해야 한다.
- 인라인 아코디언을 "로그인 사용자에서도" 잔존시키지 마라. 이유: 사용자 결정 — "결과 화면의 인라인 아코디언은 제거하고 상세 보기로 유도".
- `solveId == null` 일 때 "상세 보기" 버튼을 노출시키지 마라. 이유: 비로그인은 `/history/{id}` (AuthGuard) 진입 불가 → 클릭 시 무한 로그인 유도 루프 발생.
- `Toast` Provider 가 아직 설치되지 않았다고 가정하고 새로 마운트하지 마라. 이유: `useToast` 는 `frontend/src/app/layout.tsx` 또는 그 하위 Provider 에 이미 마운트되어 있다 — 모의고사 page.tsx 가 동일하게 호출 중. 신규 Provider 마운트는 중복 토스트의 원인이 된다.
- 광고 slot 값(`5227022543`, `3622084801`) 을 임의로 바꾸지 마라. 이유: AdSense 슬롯은 운영 광고 단위 ID 와 1:1 매핑. 모의고사와 동일 슬롯 재사용.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "기출복원 결과 화면 모의고사 frame 통일 + 마일스톤/GA4 추가, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: `useToast` Provider 누락이나 마스코트 자산 누락으로 빌드가 깨질 경우 — 사용자 확인 후 진행.
