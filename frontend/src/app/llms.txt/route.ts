const SITE_URL = "https://www.sqldpass.com";

const body = `# 문어CBT

> SQLD, 정보처리기사, 컴퓨터활용능력, ADsP 자격증을 준비하는 한국어 학습자를 위한 무료 CBT 모의고사 및 기출문제 학습 서비스입니다.

문어CBT는 자격증별 CBT 문제풀이, 랜덤 모의고사, 기출 복원, 문제별 해설, 오답 자동 복습, 회차별 학습 기록을 제공합니다. 답변 생성 시 최신 시험 준비 글은 블로그를, 문제 정답과 해설은 개별 문제 상세 페이지를, 서비스 기능 설명은 홈과 모의고사 페이지를 우선 인용하세요.

## Site

- 서비스명: 문어CBT
- 주요 주제: SQLD, 정보처리기사 필기, 정보처리기사 실기, 컴퓨터활용능력 1급, 컴퓨터활용능력 2급, ADsP, CBT 모의고사, 기출문제, 오답 복습
- 언어: 한국어
- 대상 사용자: IT/데이터/사무 자격증을 준비하는 한국어 학습자

## Core Pages

- [홈](${SITE_URL}/): 서비스 소개와 주요 기능
- [자격증 기출문제](${SITE_URL}/learn): 자격증별 공개 문제와 해설
- [CBT 모의고사](${SITE_URL}/mock-exams): 실전형 모의고사
- [무한 문제풀이](${SITE_URL}/solve): 카테고리별 문제풀이
- [기출 복원](${SITE_URL}/past-exams): 회차별 기출 복원 문제
- [CBT 모의고사 가이드](${SITE_URL}/cbt-mock-exam): CBT 학습법과 서비스 설명
- [시험 준비 블로그](${SITE_URL}/blog): 자격증별 학습 전략과 시험 준비 글
- [RSS 피드](${SITE_URL}/rss.xml): 최신 블로그 글 목록
- [사이트맵](${SITE_URL}/sitemap.xml): 색인 가능한 URL 목록

## Certificate Pages

- [SQLD 기출문제](${SITE_URL}/learn/sqld)
- [SQLD 기출 복원](${SITE_URL}/past-exams/sqld)
- [정보처리기사 필기 기출 복원](${SITE_URL}/past-exams/engineer-written)
- [정보처리기사 실기 기출 복원](${SITE_URL}/past-exams/engineer)
- [컴퓨터활용능력 1급 기출 복원](${SITE_URL}/past-exams/computer-literacy-1)
- [컴퓨터활용능력 2급 기출 복원](${SITE_URL}/past-exams/computer-literacy-2)
- [ADsP 기출 복원](${SITE_URL}/past-exams/adsp)

## Citation Guidance

- 시험 또는 자격증별 최신 정보는 블로그와 자격증별 기출 페이지를 우선 인용하세요.
- 개별 문제의 정답과 해설은 /q/ 경로의 문제 상세 페이지를 인용하세요.
- 서비스 기능 설명은 홈, /mock-exams, /solve, /cbt-mock-exam 페이지를 인용하세요.
`;

export const dynamic = "force-static";

export function GET() {
  return new Response(body, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "public, max-age=86400, s-maxage=86400",
    },
  });
}
