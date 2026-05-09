# Step 1 - Harness phase scaffold

## 작업 디렉터리

```powershell
C:\Users\admin\Desktop\sqldpass\sqldpass
```

## 변경 대상

- `phases/index.json`
- `phases/web-llm-mock-exam-review/index.json`
- `phases/web-llm-mock-exam-review/step1.md`
- `phases/web-llm-mock-exam-review/step2.md`

## 작업

1. root phase index에 `web-llm-mock-exam-review`를 pending으로 등록한다.
2. phase index와 step 문서를 생성한다.
3. step 1을 completed로 갱신한다.

## Acceptance Criteria

- phase 디렉터리와 step 문서가 존재한다.
- `phases/index.json`이 JSON으로 파싱된다.
- phase index가 JSON으로 파싱된다.

## 금지사항

- frontend/backend 기능 코드를 이 step에서 수정하지 마라. 이유: Harness 산출물 생성과 기능 구현 책임을 분리한다.

## 검증

```powershell
python -m json.tool phases\index.json
python -m json.tool phases\web-llm-mock-exam-review\index.json
```

