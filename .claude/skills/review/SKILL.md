---
name: review
description: sqldpass 변경 사항을 Harness 기준으로 리뷰한다. 사용자가 "리뷰", "검토", "변경 확인", "하네스 리뷰"를 요청할 때 사용한다.
---

# Harness Review

이 프로젝트의 변경 사항을 리뷰한다.

## 먼저 읽을 파일

- `/AGENTS.md`
- `/CLAUDE.md`
- `/frontend/AGENTS.md` 또는 `/backend/AGENTS.md` 중 변경 대상에 해당하는 문서
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`

## 체크리스트

1. 아키텍처 준수: `ARCHITECTURE.md`에 정의된 디렉토리 구조를 따르는가?
2. 기술 스택 준수: 프론트엔드는 Next.js 16/React 19/TypeScript/Tailwind CSS 4, 백엔드는 Spring Boot 4/Java 21/Gradle 기준을 벗어나지 않았는가?
3. 작업 경계 준수: 프론트엔드 변경은 `frontend/`, 백엔드 변경은 `backend/` 안에 머무르며, API 계약 변경은 양쪽에 반영되었는가?
4. 테스트 또는 검증 존재: 변경 범위에 맞는 검증 명령이 실행되었는가?
5. CRITICAL 규칙: `CLAUDE.md`의 CRITICAL 규칙을 위반하지 않았는가?

## 검증 명령

프론트엔드 변경:

```powershell
cd frontend
npm run lint
npm run build
```

백엔드 변경:

```powershell
cd backend
.\\gradlew.bat test
.\\gradlew.bat compileJava
```

## 출력 형식

| 항목 | 결과 | 비고 |
|------|------|------|
| 아키텍처 준수 | OK/FAIL | {상세} |
| 기술 스택 준수 | OK/FAIL | {상세} |
| 작업 경계 준수 | OK/FAIL | {상세} |
| 테스트 또는 검증 | OK/FAIL | {상세} |
| CRITICAL 규칙 | OK/FAIL | {상세} |

위반 사항이 있으면 수정 방안을 구체적으로 제시한다.
