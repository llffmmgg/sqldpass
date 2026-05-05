// 개별 Agent Skill 본문(SKILL.md).
// 본문은 skills-data.ts의 SKILLS 배열에서 가져온다.

import { getSkillBySlug } from "../../skills-data";

export function GET(_req: Request, ctx: { params: Promise<{ slug: string }> }): Promise<Response> {
  return ctx.params.then(({ slug }) => {
    const skill = getSkillBySlug(slug);
    if (!skill) {
      return new Response("Not Found", {
        status: 404,
        headers: { "Content-Type": "text/plain; charset=utf-8" },
      });
    }
    return new Response(skill.body, {
      status: 200,
      headers: {
        "Content-Type": "text/markdown; charset=utf-8",
        "Cache-Control": "public, max-age=3600, s-maxage=3600",
      },
    });
  });
}
