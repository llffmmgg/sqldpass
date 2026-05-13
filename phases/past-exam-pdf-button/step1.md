# Step 1 — 기출 풀이 화면 PDF 버튼 + 인쇄 페이지 PAST_EXAM 분기

## 배경

기출(`/past-exams/[id]`) 화면에는 PDF 다운로드 버튼이 없어 사용자가 PDF를 받을 수 없다. 모의고사는 `MockExamPdfButton`이 풀이 화면 곳곳에 노출되지만 기출은 `PastExamRunnerClient.tsx`에 해당 버튼이 import 되지 않은 상태.

조사 결과 **백엔드는 추가 작업 불필요**:
- `mock_exam` 테이블에 `kind=PAST_EXAM`으로 통합 저장. `MockExamPdfService.buildFilename()` 은 이미 PAST_EXAM 분기를 갖고 있어 자동으로 `SQLD_기출_2024_56회.pdf` 형식 파일명 생성
- `/api/mock-exams/{id}/pdf/download` 와 `/pdf/eligibility` 엔드포인트는 `kind` 검증 없이 mockExamId만 받아 동작
- `PrintMockExamResponse` 는 이미 `MockExamKind kind` 필드(L28) + `examYear`/`examRound` 필드(L29-30)를 가지고 있어, 인쇄 페이지에 `kind` 값이 그대로 직렬화돼 전달됨

남은 작업은 **프론트엔드 두 곳**.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/app/print/mock-exams/[id]/page.tsx` | 표지에 `kind === "PAST_EXAM"` 분기 추가 — "모의고사." → "기출.", `MOCK EXAM` → `PAST EXAM`, `VOL.{sequence}` → `{examYear}.{examRound}회` |
| `frontend/src/components/past-exams/PastExamRunnerClient.tsx` | `MockExamPdfButton` import + 풀이 진행 화면 헤더 박스(L308-321) 와 결과 화면(`PastExamResultView`) 두 곳에 버튼 배치 |

## 프린트 페이지 변경 상세

`frontend/src/app/print/mock-exams/[id]/page.tsx`:

1. `PrintMockExam` 인터페이스(L52-62)에 `kind?: "AI" | "PAST_EXAM"` 필드 추가
2. 표지 렌더 블록(L297-338) 분기:
   - `const isPast = data.kind === "PAST_EXAM";` 도입
   - `meta.eng` 라벨: `isPast`이면 자격증별 `PAST_EXAM` 라벨로 치환 (`SQLD PAST EXAM`, `ENGINEER WRITTEN PAST EXAM`, ... 같은 식). 별도 매핑 객체(`COVER_META_PAST_ENG`) 추가
   - `volume` 라벨(L306): `isPast && data.examYear && data.examRound` 이면 `${data.examYear}.${data.examRound}회` 형식, 아니면 기존 `VOL.${data.sequence}`
   - cover-sub 텍스트(L322): `isPast ? "기출" : "모의고사"` + 점

3. 모의고사 표지는 회귀 없도록 기존 출력 그대로 유지 (분기로만 추가)

## PastExamRunnerClient 변경 상세

`frontend/src/components/past-exams/PastExamRunnerClient.tsx`:

1. import 추가: `import MockExamPdfButton from "@/components/MockExamPdfButton";`
2. 풀이 진행 화면 헤더 박스(L308-321) 내부에 PDF 버튼 배치:
   - 박스 안의 제목/설명 옆에 자연스러운 위치(우측 정렬 또는 설명 하단)에 `<MockExamPdfButton examId={exam.id} />` 삽입
   - `flex flex-wrap items-center justify-between` 같은 기존 레이아웃 패턴과 조화
3. 결과 화면 `PastExamResultView` (L490~) 의 헤더(`기출 복원 결과` 제목 영역)에 PDF 버튼 추가
4. `exam.id` 타입은 `number` 라 `MockExamPdfButton` 의 `examId: number` 시그니처와 호환

`MockExamPdfButton` 은 직전 phase에서 게이트 풀어둔 상태라:
- 비로그인 → `alert("로그인 후 이용 가능합니다.")`
- 미구독 → 기존 "Lifetime 플랜 전용..." 토스트
- UNLIMITED → 정상 다운로드

기출에도 같은 분기 그대로 동작 → 추가 가드 불필요.

## 손대지 않는 것

- 백엔드 코드 전부: `PrintMockExamResponse` 는 이미 필요한 필드를 갖고 있음. `MockExamPdfService`/`PdfRenderService`/`MockExamController` 모두 그대로
- `MockExamPdfButton.tsx`: 컴포넌트 시그니처(`examId: number`) 변경 없음
- `SubscriptionService.allowsPdf()`: Lifetime 정책 유지 — 기출에도 동일 적용
- R2 키 컨벤션: `pdf/mock-exams/{id}/{hash}.pdf` 그대로 (콘텐츠 해시에 `kind`/`examYear`/`examRound`가 이미 포함돼 캐시 분리됨)
- 인터스티셜 화면(`MockExamAttemptsView`): 이미 모의고사에서 PDF 버튼이 노출되는 공유 컴포넌트라면 자동으로 기출에도 적용. 별도 수정 불요

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 확인 (`npm run dev` 후):
1. 비로그인으로 `/past-exams/{기출ID}` 진입 → 헤더 박스에서 PDF 버튼 노출 → 클릭 시 `alert("로그인 후 이용 가능합니다.")`
2. 로그인 + 미구독 → 클릭 시 "Lifetime 플랜 전용..." 토스트
3. 로그인 + UNLIMITED 구독 → 클릭 시 PDF 다운로드. 파일명이 `{자격증}_기출_{년도}_{회차}회.pdf` 인지 확인. 열어서 표지에 "기출." + 영문 라벨 `PAST EXAM` + 회차 라벨 `{년도}.{회차}회` 표시 확인
4. 회귀: `/mock-exams/{모의고사ID}` 에서 PDF 다운 → 표지가 여전히 "모의고사." 로 나오는지 확인 (분기가 모의고사 출력에 영향 없는지)
5. DevTools Network 에서 기출 PDF 다운로드 시 `/api/mock-exams/{기출ID}/pdf/download` 호출 확인 (엔드포인트 재사용)

## Acceptance Criteria

1. `PastExamRunnerClient` 의 풀이 진행 화면과 결과 화면 두 곳에서 PDF 버튼이 보인다.
2. 비로그인/미구독/구독 3 케이스 모두 `MockExamPdfButton` 의 기존 분기 그대로 동작한다.
3. UNLIMITED 구독자가 받은 기출 PDF 표지에 "기출." 텍스트와 `PAST EXAM` 영문 라벨, `{년도}.{회차}회` 형식 라벨이 표시된다.
4. 모의고사 PDF 표지는 변경 전과 동일하게 "모의고사."/`MOCK EXAM`/`VOL.{sequence}` 로 나온다 (회귀 없음).
5. `npm run lint`, `npm run build` 통과.

## 금지 사항

- 백엔드 코드(`PrintMockExamResponse`, `MockExamPdfService`, `MockExamController` 등)를 수정하지 마라. **이유**: 필요한 모든 필드와 분기가 이미 구현돼 있음. 백엔드 변경은 회귀 위험만 키운다.
- `MockExamPdfButton.tsx` 를 수정하지 마라. **이유**: 직전 phase 에서 게이트를 풀어 비로그인/미구독/구독 분기가 이미 정리됨. 기출은 같은 컴포넌트를 import만 하면 됨.
- 새 토스트/alert 문구를 만들지 마라. **이유**: PDF 버튼 클릭 시 안내는 사이트의 다른 가드와 동일하게 `BookmarkButton` 패턴(alert)/기존 토스트 그대로 둔다.
- `MockExamAttemptsView` 의 PDF 버튼 노출 로직을 별도로 조작하지 마라. **이유**: 모의고사·기출이 같은 공유 컴포넌트를 쓰므로, 그 컴포넌트 내부 PDF 버튼은 mockExamId 기반으로 이미 통일 동작한다.
- `examYear`/`examRound` 가 null 인 기출 회차에 대해서 라벨 분기를 강제하지 마라. **이유**: 시드 데이터 누락 가능성 — `isPast && examYear && examRound` 모두 truthy 일 때만 라벨 치환. 그 외에는 fallback 으로 `VOL.{sequence}` 유지.

## Status 규칙

- 성공: step 1 `completed`, summary "기출 풀이 화면 헤더+결과 화면에 MockExamPdfButton 배치 + print 페이지 PAST_EXAM 표지 분기 (kind/영문라벨/cover-sub/volume), lint/build OK".
- 실패: 3회 재시도 후 `error`.
