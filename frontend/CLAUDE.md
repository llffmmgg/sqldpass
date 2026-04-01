# Frontend — CLAUDE.md

## Tech Stack

- **Next.js 16.2.2** (App Router, src/ 디렉토리 구조)
- **React 19**, **TypeScript 5**
- **Tailwind CSS 4** (PostCSS 플러그인)
- **ESLint** (eslint-config-next)

## Build & Run Commands

모든 명령은 `frontend/` 디렉토리에서 실행.

```bash
# 개발 서버
cd frontend && npm run dev

# 프로덕션 빌드
cd frontend && npm run build

# 프로덕션 서버
cd frontend && npm run start

# 린트
cd frontend && npm run lint
```

## Architecture

- App Router 기반 (`src/app/`)
- 페이지: `src/app/**/page.tsx`
- 레이아웃: `src/app/**/layout.tsx`
- 정적 파일: `public/`
- 백엔드 API: Spring Boot 서버 (`backend/`)와 통신

## Coding Style

- 컴포넌트: PascalCase 파일명 (`QuestionList.tsx`)
- 유틸/훅: camelCase 파일명 (`useQuestions.ts`)
- CSS: Tailwind 클래스 우선, 인라인 스타일 지양
- import alias: `@/*` → `src/*`
