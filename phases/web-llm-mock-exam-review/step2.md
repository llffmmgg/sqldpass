# Step 2 - Frontend web LLM review workbench

## 작업 디렉터리

```powershell
C:\Users\admin\Desktop\sqldpass\sqldpass\frontend
```

## 변경 대상

- `frontend/src/app/admin/mock-exams/[id]/page.tsx`

## 작업

1. 기존 API 기반 `AI 검증`, `AI 전체 검증` 버튼과 `verifyAllQuestions` 호출 흐름을 제거한다.
2. `웹 LLM 검수` 버튼을 추가한다.
3. 현재 모의고사 문제를 `id`, `order`, `subjectId`, `subjectName`, `questionType`, `content`, `correctOption`, `answer`, `keywords`, `explanation`, `summary`를 포함한 JSON 배열로 자동 생성한다.
4. Claude에는 원본 JSON만, ChatGPT에는 Claude 수정본 JSON만 넘길 수 있도록 복사 버튼과 textarea를 제공한다.
5. ChatGPT 최종 JSON을 파싱/검증하고 원본 대비 변경 문항 수와 차단 사유를 표시한다.
6. 최종 JSON을 diff 확인 후 기존 `updateQuestion` API로 일괄 적용하고, 성공 후 `markMockExamVerified`를 호출한다.

## Acceptance Criteria

- 모의고사 상세 화면에 기존 `AI 검증`, `AI 전체 검증` 버튼이 보이지 않는다.
- `웹 LLM 검수` 패널에서 원본 JSON을 자동 생성하고 복사할 수 있다.
- Claude 수정본 JSON을 붙여넣으면 그 JSON을 ChatGPT 전달용으로 복사할 수 있다.
- 최종 JSON 검증은 문항 수, `id`, `order`, `subjectId`, `questionType`, 필수 필드 오류를 차단한다.
- 유효한 최종 JSON은 일괄 적용되고 문제 목록이 새로고침된다.

## 금지사항

- 백엔드 API를 새로 만들지 마라. 이유: 기존 `getQuestion`, `updateQuestion`, `markMockExamVerified`로 구현 가능하다.
- LLM에 별도 프롬프트를 추가하지 마라. 이유: Claude/ChatGPT 프로젝트 내부 지침이 검수 정책을 책임진다.
- `expertVerified` 토글을 자동으로 변경하지 마라. 이유: 모의고사 공개 승인과 문제 검수 완료는 별도 운영 결정이다.

## 검증

```powershell
npm run lint
npm run build
```

