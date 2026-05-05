// Agent Skills 본문 데이터.
// 추가 스킬을 늘리면 SKILLS 배열에 push만 하면 index.json과 SKILL.md route 양쪽이 동기화된다.

export interface Skill {
  slug: string;
  name: string;
  description: string;
  body: string;
}

const RANDOM_QUESTION_BODY = `# 자격증 랜덤 문제 가져오기

자격증을 지정해 무작위 공개 문제 한 건을 가져온다.

## When to use

- 사용자가 SQLD/정처기 필기/정처기 실기/컴활 1급/컴활 2급/ADsP 중 하나의 자격증으로 연습 문제를 요청할 때
- 자격증 자가진단·간단 퀴즈를 진행할 때
- 사용자가 "오늘의 한 문제" 류 짧은 학습 세션을 원할 때

## Tool

\`\`\`json
{
  "name": "fetch_random_question",
  "description": "공개된 자격증 문제 한 건을 무작위로 가져온다.",
  "endpoint": "GET https://www.sqldpass.com/api/public/solve/random",
  "query_parameters": {
    "examType": {
      "type": "string",
      "enum": [
        "SQLD",
        "ENGINEER_WRITTEN",
        "ENGINEER_PRACTICAL",
        "COMPUTER_LITERACY_1",
        "COMPUTER_LITERACY_2",
        "ADSP"
      ],
      "required": true
    },
    "categoryId": {
      "type": "integer",
      "required": false,
      "description": "카테고리 ID로 추가 필터링"
    }
  },
  "response_format": "application/json (PublicSolveQuestionResponse)"
}
\`\`\`

## Notes

- 공개 API이므로 인증 토큰이 필요 없다
- 응답에는 \`content\`, \`questionType\`, \`options\`, \`correctOption\`, \`explanation\` 등이 포함된다
- 문제 표시는 마크다운으로 렌더링하라 — 코드 블록(SQL/C/Java/Python)을 보존해야 한다
`;

const PAST_EXAM_BODY = `# 회차별 기출 가져오기

자격증과 회차를 지정해 기출 복원 시험을 가져온다.

## When to use

- 사용자가 특정 회차 기출(예: "2025년 3회 정처기 필기")을 요청할 때
- 회차별 학습 진단을 진행할 때

## Tool

\`\`\`json
{
  "name": "list_past_exams",
  "description": "자격증별 공개 기출 시험 목록을 가져온다.",
  "endpoint": "GET https://www.sqldpass.com/api/public/past-exams",
  "query_parameters": {
    "examType": {
      "type": "string",
      "enum": [
        "SQLD",
        "ENGINEER_WRITTEN",
        "ENGINEER_PRACTICAL",
        "COMPUTER_LITERACY_1",
        "COMPUTER_LITERACY_2",
        "ADSP"
      ],
      "required": true
    }
  }
}
\`\`\`

\`\`\`json
{
  "name": "fetch_past_exam_detail",
  "description": "특정 기출 시험의 전체 문제와 정답을 가져온다.",
  "endpoint": "GET https://www.sqldpass.com/api/public/past-exams/{id}",
  "path_parameters": {
    "id": {
      "type": "integer",
      "required": true
    }
  }
}
\`\`\`

## Notes

- 기출 풀이 후 자동 채점은 \`POST /api/public/past-exams/{id}/grade\`로 가능
- 답변 채점은 클라이언트에서 \`PastExamGradeRequest\`로 보낸다
`;

const RANKING_BODY = `# 학습 랭킹 조회

전체 사용자 중 학습 활동이 활발한 닉네임 순위를 가져온다.

## When to use

- 사용자가 학습 동기 부여를 위한 비교 정보를 원할 때
- "오늘 가장 많이 푼 사용자" 류 질문에 답할 때

## Tool

\`\`\`json
{
  "name": "fetch_ranking",
  "description": "최근 학습 활동 랭킹을 가져온다.",
  "endpoint": "GET https://www.sqldpass.com/api/public/ranking",
  "response_format": "application/json (PublicRankingResponse)"
}
\`\`\`

## Notes

- 닉네임만 노출되며 개인 식별 정보는 포함되지 않는다
`;

export const SKILLS: Skill[] = [
  {
    slug: "fetch-random-question",
    name: "fetch-random-question",
    description: "자격증을 지정해 sqldpass의 공개 문제 한 건을 무작위로 가져온다",
    body: RANDOM_QUESTION_BODY,
  },
  {
    slug: "past-exams",
    name: "past-exams",
    description: "자격증·회차별 기출 복원 시험을 목록·상세·자동 채점 형태로 사용한다",
    body: PAST_EXAM_BODY,
  },
  {
    slug: "fetch-ranking",
    name: "fetch-ranking",
    description: "sqldpass 학습 활동 랭킹(닉네임 단위)을 가져온다",
    body: RANKING_BODY,
  },
];

export function getSkillBySlug(slug: string): Skill | undefined {
  return SKILLS.find((s) => s.slug === slug);
}
