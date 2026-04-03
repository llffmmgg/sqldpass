# Google OAuth 로그인 설정 가이드

## 1. Google Cloud Console 설정

- [ ] [console.cloud.google.com](https://console.cloud.google.com) 접속
- [ ] 프로젝트 생성 (또는 기존 프로젝트 선택)
- [ ] **API 및 서비스 → OAuth 동의 화면** 설정
  - 사용자 유형: 외부
  - 앱 이름, 지원 이메일 입력
  - 범위: `openid`, `profile` 추가
  - 테스트 사용자에 본인 Google 계정 추가
- [ ] **API 및 서비스 → 사용자 인증 정보 → OAuth 2.0 클라이언트 ID** 생성
  - 애플리케이션 유형: 웹 애플리케이션
  - 승인된 리디렉션 URI: `http://localhost:3000/auth/callback/google`

## 2. 환경변수 설정

- [ ] 백엔드 `.env` 파일에 추가:
  ```
  GOOGLE_OAUTH_CLIENT_ID=발급받은_클라이언트_ID
  GOOGLE_OAUTH_CLIENT_SECRET=발급받은_클라이언트_시크릿
  ```
- [ ] 프론트엔드 `.env.local` 파일에 추가:
  ```
  NEXT_PUBLIC_GOOGLE_CLIENT_ID=발급받은_클라이언트_ID
  ```

## 3. 실행 및 확인

- [ ] `cd backend && ./gradlew bootRun`
- [ ] `cd frontend && npm run dev`
- [ ] 사이트 접속 → 로그인 버튼 클릭 → Google 로그인 → 홈으로 리다이렉트 확인
- [ ] NavBar에 닉네임 표시 확인
- [ ] 풀이 기록, 오답 노트 페이지 정상 동작 확인
