# Codex in-app browser runbook

## 목적

admin 모의고사 상세 화면의 웹 LLM 검수 패널에서 자동 실행 패키지를 읽고, Claude 프로젝트와 ChatGPT 프로젝트를 직렬로 호출한 뒤 최종 JSON을 admin 화면에 다시 채운다.

## 전제

- 운영자는 admin 모의고사 상세 화면에서 `웹 LLM 검수`를 연다.
- Claude 프로젝트 URL과 ChatGPT 프로젝트 URL은 한 번 저장되어 있어야 한다.
- 각 프로젝트 내부 instruction은 "JSON을 받으면 전체 수정본 JSON을 반환"하도록 이미 설정되어 있다.
- Claude/ChatGPT에는 별도 프롬프트를 붙이지 않는다.

## admin에서 읽을 대상

- 자동 실행 패키지: `data-testid="web-review-automation-package"`
- 실행 명령: `data-testid="web-review-codex-run-command"`
- 최종 JSON 입력칸: `data-testid="web-review-final-json"`
- 최종 JSON 검증 버튼: `data-testid="web-review-validate-final"`
- 최종 적용 버튼: `data-testid="web-review-apply-final"`

## 실행 순서

1. admin 화면에서 `data-testid="web-review-automation-package"` 값을 읽는다.
2. 패키지의 `status`가 `blocked`이면 `errors`를 운영자에게 보고하고 중단한다.
3. `claudeProjectUrl`을 연다.
4. Claude 입력창을 찾아 `originalJson` 값만 그대로 전송한다.
5. Claude 응답이 완료될 때까지 기다린다.
6. Claude 응답에서 JSON 배열 전체를 추출한다.
   - 코드블록이 있으면 코드블록 내용을 우선 사용한다.
   - 코드블록이 없으면 응답 본문에서 첫 `[`부터 마지막 `]`까지의 JSON 배열을 사용한다.
7. `chatgptProjectUrl`을 연다.
8. ChatGPT 입력창을 찾아 Claude 응답 JSON만 그대로 전송한다.
9. ChatGPT 응답이 완료될 때까지 기다린다.
10. ChatGPT 응답에서 JSON 배열 전체를 추출한다.
11. `adminUrl`로 돌아온다.
12. `data-testid="web-review-final-json"` 입력칸에 ChatGPT 최종 JSON을 채운다.
13. `data-testid="web-review-validate-final"` 버튼을 누른다.
14. 검증 결과를 운영자에게 보고한다.

## 금지 사항

- Claude/ChatGPT에 추가 프롬프트를 붙이지 않는다.
- `data-testid="web-review-apply-final"` 버튼을 누르지 않는다.
- Claude/ChatGPT 웹 UI의 CSS class를 장기 의존 대상으로 기록하지 않는다.
- 프로젝트 URL을 repo, 서버, DB에 저장하지 않는다.

## 실패 처리

- Claude 또는 ChatGPT가 JSON이 아닌 설명을 반환하면, 코드블록 또는 JSON 배열 부분만 추출해서 다음 단계에 사용한다.
- JSON 배열을 찾을 수 없으면 해당 응답 원문 일부와 함께 중단한다.
- admin 검증이 실패하면 적용하지 않고 검증 실패 메시지를 운영자에게 보고한다.
