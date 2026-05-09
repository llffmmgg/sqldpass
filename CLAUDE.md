# 프로젝트: sqldpass

## 기술 스택
- 모노레포: `frontend/`, `backend/`
- 프론트엔드: Next.js 16, React 19, TypeScript, Tailwind CSS 4
- 백엔드: Spring Boot 4, Java 21, Gradle

## 아키텍처 규칙
- CRITICAL: 루트 `AGENTS.md`를 먼저 읽고, 변경 대상에 따라 `frontend/AGENTS.md` 또는 `backend/AGENTS.md`를 추가로 읽을 것
- CRITICAL: 프론트엔드 변경은 기본적으로 `frontend/` 안에서, 백엔드 변경은 기본적으로 `backend/` 안에서 수행할 것
- CRITICAL: API 계약을 변경하는 작업은 프론트엔드와 백엔드를 한 번에 맞추거나 남은 불일치를 명확히 기록할 것
- CRITICAL: 실제 DB 계정 정보, 로컬 자격 증명, `.env` 파일, 비밀번호를 커밋하지 말 것
- Harness phase는 루트에서 관리하되, step의 작업 경로와 검증 명령은 앱별로 명시할 것
- 프론트엔드 컴포넌트, 타입, API 클라이언트는 기존 `frontend/src` 구조를 따를 것
- 백엔드 컨트롤러, 서비스, 도메인, 영속성 코드는 기존 `backend/src/main/java/com/sqldpass` 구조를 따를 것

## 개발 프로세스
- CRITICAL: 동작 변경에는 가능한 범위에서 테스트 또는 빌드 검증을 포함할 것
- 프론트엔드 작업은 `frontend/`에서 `npm run lint`, `npm run build`를 기준 검증으로 사용한다
- 백엔드 작업은 `backend/`에서 `.\gradlew.bat test` 또는 `.\gradlew.bat compileJava`를 기준 검증으로 사용한다
- 커밋 메시지는 한국어로 작성하되 conventional commits 접두어를 유지할 것 (예: `feat: 로그인 유효성 검사 추가`)

## 명령어
```powershell
cd frontend
npm run dev      # 프론트엔드 개발 서버
npm run lint     # 프론트엔드 ESLint
npm run build    # 프론트엔드 프로덕션 빌드
```

```powershell
cd backend
.\gradlew.bat test         # 백엔드 테스트
.\gradlew.bat compileJava  # 백엔드 컴파일
```
