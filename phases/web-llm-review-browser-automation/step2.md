# Step 2 - Frontend project URL persistence and Codex automation package

## 작업 디렉터리

```powershell
C:\Users\admin\Desktop\sqldpass\sqldpass\frontend
```

## 변경 대상

- `frontend/src/app/admin/mock-exams/[id]/page.tsx`

## 작업

1. 웹 LLM 검수 패널에 Claude/ChatGPT 프로젝트 URL 입력칸을 추가한다.
2. URL은 `localStorage`에 저장하고 모든 모의고사 상세 페이지에서 재사용한다.
3. Codex 인앱 브라우저가 읽을 수 있는 자동 실행 패키지를 생성/복사한다.
4. 브라우저 자동화가 안정적으로 찾을 수 있도록 주요 컨트롤에 `data-testid`를 추가한다.

## Acceptance Criteria

- URL 최초 저장 후 다른 모의고사 상세 페이지에서도 복원된다.
- 자동 실행 패키지에는 admin URL, Claude URL, ChatGPT URL, 원본 JSON이 포함된다.
- URL이 비어 있으면 자동 실행 패키지 복사를 차단한다.
- `npm run lint`, `npm run build`가 통과한다.

## 금지사항

- 프로젝트 URL을 서버/DB/repo에 저장하지 마라. 이유: 개인 계정의 웹 프로젝트 URL은 운영자 브라우저 설정이다.
- LLM에 보낼 프롬프트를 추가하지 마라. 이유: 프로젝트 내부 지침이 검수 정책을 책임진다.
- 최종 일괄 적용 버튼을 Codex 자동 실행 기본 동작에 포함하지 마라. 이유: 운영자가 diff를 보고 직접 승인해야 한다.

