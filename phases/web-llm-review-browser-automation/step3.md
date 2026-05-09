# Step 3 - Frontend browser run package exposure and runbook

## 작업 디렉터리

```powershell
C:\Users\admin\Desktop\sqldpass\sqldpass\frontend
```

## 변경 대상

- `frontend/src/app/admin/mock-exams/[id]/page.tsx`
- 필요 시 `docs/` 또는 `phases/web-llm-review-browser-automation/` 아래 실행 문서

## 목적

Codex 인앱 브라우저가 admin 모의고사 상세 화면에서 자동 실행 정보를 직접 읽고, 사용자가 매번 JSON이나 URL을 수동으로 옮기지 않아도 `Claude 프로젝트 -> ChatGPT 프로젝트 -> admin 최종 JSON 입력` 흐름을 수행할 수 있게 한다.

## 작업

1. 웹 LLM 검수 패널에 자동 실행 패키지를 화면/DOM에 노출한다.
   - `data-testid="web-review-automation-package"`를 가진 readonly 영역을 추가한다.
   - 패키지에는 `adminUrl`, `claudeProjectUrl`, `chatgptProjectUrl`, `originalJson`, `target` selector, 실행 순서를 포함한다.
   - URL이 비어 있으면 패키지에는 명확한 오류 상태를 표시하고 실행 복사를 막는다.
2. Codex가 바로 사용할 수 있는 실행 명령 텍스트를 제공한다.
   - 예: "이 패키지를 읽고 Claude 프로젝트에 originalJson만 전송한 뒤, 응답 JSON을 ChatGPT 프로젝트에 그대로 전송하고, 최종 응답을 admin의 최종 JSON 입력칸에 채운 뒤 검증 버튼을 눌러줘."
   - 이 명령 텍스트도 `data-testid="web-review-codex-run-command"`로 노출한다.
3. 실행 후 admin 복귀를 안정화한다.
   - 최종 JSON 입력칸은 `data-testid="web-review-final-json"`를 유지한다.
   - 검증 버튼은 `data-testid="web-review-validate-final"`를 유지한다.
   - 적용 버튼은 운영자가 diff와 validation을 확인한 뒤 누르는 수동 단계로 유지한다.
4. 브라우저 자동화 runbook을 문서화한다.
   - Codex 인앱 브라우저가 수행해야 할 실제 순서를 단계별로 적는다.
   - Claude/ChatGPT 웹 UI 셀렉터는 서비스 UI가 바뀔 수 있으므로 고정 셀렉터 대신 "입력창 탐색, JSON 전송, 응답 코드블록/본문 추출" 기준으로 작성한다.

## Acceptance Criteria

- 프로젝트 URL을 한 번 저장하면 다른 모의고사 상세 페이지에서도 복원된다.
- admin 화면에서 Codex가 읽을 수 있는 자동 실행 패키지 DOM이 존재한다.
- 자동 실행 패키지는 원본 문제 JSON을 프롬프트 없이 그대로 포함한다.
- 실행 명령 텍스트는 Claude -> ChatGPT -> admin 순서를 명확히 지시한다.
- 최종 적용은 자동으로 하지 않고 운영자 확인 후 수동으로만 가능하다.
- `npm run lint`와 `npm run build`가 통과한다.

## 금지 사항

- Claude/ChatGPT 프로젝트 URL을 서버, DB, repo에 저장하지 마라. 이유: 개인 계정별 웹 프로젝트 URL은 브라우저 로컬 설정이어야 한다.
- LLM에 보낼 프롬프트를 추가하지 마라. 이유: 검수 지침은 각 프로젝트 내부 instruction이 책임진다.
- ChatGPT 최종 응답을 자동으로 DB에 적용하지 마라. 이유: 운영자가 diff와 validation 결과를 보고 직접 승인해야 한다.
- Claude/ChatGPT 웹 UI의 변동 가능한 CSS class에 의존하지 마라. 이유: 외부 서비스 UI 변경 시 자동화가 쉽게 깨진다.

## 검증 명령

```powershell
npm run lint
npm run build
```
