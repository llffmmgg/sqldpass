// Agent Skills Discovery RFC v0.2.0
// 스킬 본문은 별도 route(/.well-known/agent-skills/<slug>/SKILL.md)에서 제공.
// digest는 동일한 본문 함수에서 SHA-256 계산.

import { createHash } from "node:crypto";
import { SKILLS } from "../skills-data";

const SITE_URL = "https://www.sqldpass.com";

export function GET(): Response {
  const skills = SKILLS.map((skill) => {
    const digest = createHash("sha256").update(skill.body, "utf8").digest("hex");
    return {
      name: skill.name,
      type: "skill",
      description: skill.description,
      url: `${SITE_URL}/.well-known/agent-skills/${skill.slug}/SKILL.md`,
      digest: `sha256:${digest}`,
    };
  });

  const body = {
    $schema:
      "https://raw.githubusercontent.com/cloudflare/agent-skills-discovery-rfc/main/schemas/v0.2.0/index.schema.json",
    name: "문어CBT — 자격증 학습 에이전트 스킬",
    description:
      "SQLD/정처기/컴활/ADsP 모의고사·기출 데이터를 자동으로 가져오고 추천하는 스킬 모음",
    skills,
  };

  return new Response(JSON.stringify(body, null, 2), {
    status: 200,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "public, max-age=3600, s-maxage=3600",
    },
  });
}
