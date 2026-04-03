import ScrollReveal from "@/components/ScrollReveal";

export default function Home() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      {/* Hero */}
      <section className="relative overflow-hidden">
        {/* Layer 1: Ambient base gradient — fills the void */}
        <div className="hero-ambient" aria-hidden="true" />

        {/* Layer 2: Grid pattern */}
        <div className="absolute inset-0 grid-pattern" />

        {/* Layer 3: Animated gradient orbs — 5 overlapping layers */}
        <div className="hero-orb hero-orb-1" aria-hidden="true" />
        <div className="hero-orb hero-orb-2" aria-hidden="true" />
        <div className="hero-orb hero-orb-3" aria-hidden="true" />
        <div className="hero-orb hero-orb-4" aria-hidden="true" />
        <div className="hero-orb hero-orb-5" aria-hidden="true" />

        {/* Layer 4: Light sweep beams */}
        <div className="hero-beam hero-beam-1" aria-hidden="true" />
        <div className="hero-beam hero-beam-2" aria-hidden="true" />

        {/* Layer 5: Noise texture */}
        <div className="noise-overlay" aria-hidden="true" />

        {/* Layer 6: Edge vignette for readability */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_50%_50%,transparent_0%,var(--background)_100%)]" />

        {/* Layer 7: SQL fragments scattered in margins */}
        {/* Left side */}
        <span className="sql-frag sql-float-1 hidden lg:block absolute top-[12%] left-[3%] font-mono text-[15px] text-foreground/[0.3] rotate-[-4deg] select-none pointer-events-none" aria-hidden="true">
          SELECT *
        </span>
        <span className="sql-frag sql-float-2 hidden lg:block absolute top-[35%] left-[5%] font-mono text-[11px] text-foreground/[0.22] rotate-[2deg] select-none pointer-events-none" aria-hidden="true">
          WHERE dept_id = 10
        </span>
        <span className="sql-frag sql-float-3 hidden lg:block absolute top-[58%] left-[2%] font-mono text-[13px] text-foreground/[0.25] rotate-[-2deg] select-none pointer-events-none" aria-hidden="true">
          GROUP BY name
        </span>
        <span className="sql-frag sql-float-4 hidden lg:block absolute top-[78%] left-[6%] font-mono text-[12px] text-foreground/[0.2] rotate-[5deg] select-none pointer-events-none" aria-hidden="true">
          INNER JOIN
        </span>
        {/* Right side */}
        <span className="sql-frag sql-float-5 hidden lg:block absolute top-[18%] right-[4%] font-mono text-[13px] text-foreground/[0.25] rotate-[3deg] select-none pointer-events-none" aria-hidden="true">
          ORDER BY 1 DESC
        </span>
        <span className="sql-frag sql-float-6 hidden lg:block absolute top-[42%] right-[2%] font-mono text-[15px] text-foreground/[0.3] rotate-[-3deg] select-none pointer-events-none" aria-hidden="true">
          COUNT(*)
        </span>
        <span className="sql-frag sql-float-7 hidden lg:block absolute top-[65%] right-[5%] font-mono text-[11px] text-foreground/[0.22] rotate-[4deg] select-none pointer-events-none" aria-hidden="true">
          HAVING SUM &gt; 50000
        </span>
        <span className="sql-frag sql-float-8 hidden lg:block absolute top-[85%] right-[3%] font-mono text-[12px] text-foreground/[0.25] rotate-[-5deg] select-none pointer-events-none" aria-hidden="true">
          LEFT OUTER JOIN
        </span>

        <div className="relative mx-auto max-w-5xl px-4 py-32 text-center sm:px-6 sm:py-40 lg:px-8">
          <ScrollReveal>
            <span className="inline-block rounded-full border border-violet-500/30 bg-violet-500/10 px-4 py-1.5 text-sm font-medium text-violet-400">
              현재 개발 중
            </span>
          </ScrollReveal>

          <ScrollReveal delay={1}>
            <h1 className="mt-6 text-3xl font-bold tracking-tight sm:text-4xl md:text-5xl lg:text-6xl">
              <span className="bg-gradient-to-r from-amber-400 via-amber-300 to-amber-500 bg-clip-text text-transparent">
                SQLD 합격
              </span>
              ,
              <br />
              문제 풀이로 완성하세요
            </h1>
          </ScrollReveal>

          <ScrollReveal delay={2}>
            <p className="mx-auto mt-6 max-w-2xl text-base text-muted sm:text-lg">
              실제 시험과 동일한 유형의 문제를 풀고, 오답을 분석하고,
              <br className="hidden sm:block" />
              취약 과목을 집중 학습하세요.
            </p>
          </ScrollReveal>

          <ScrollReveal delay={3}>
            <div className="mt-10 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
              <a
                href="/solve"
                className="btn-glow inline-flex items-center rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-zinc-900 transition-all duration-300 hover:bg-primary-hover hover:scale-[1.03]"
              >
                무료로 시작하기
              </a>
              <a
                href="#preview"
                className="inline-flex items-center rounded-lg border border-border px-6 py-3 text-sm font-semibold text-foreground transition-all duration-300 hover:border-violet-500/50 hover:bg-violet-500/5"
              >
                문제 미리보기
              </a>
            </div>
          </ScrollReveal>
        </div>
      </section>

      {/* Features */}
      <section className="mx-auto max-w-5xl px-4 py-24 sm:px-6 lg:px-8">
        <ScrollReveal>
          <h2 className="text-center text-2xl font-bold sm:text-3xl">
            주요 기능
          </h2>
          <p className="mt-3 text-center text-muted">
            합격에 필요한 모든 것을 한 곳에서
          </p>
        </ScrollReveal>

        <div className="mt-12 grid grid-cols-1 gap-6 lg:grid-cols-3">
          {/* Feature 1 */}
          <ScrollReveal delay={1}>
            <div className="group relative rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:-translate-y-1.5 hover:border-amber-500/30 hover:shadow-[0_0_24px_var(--glow)]">
              <span className="absolute top-4 right-4 text-[64px] font-bold leading-none text-foreground/[0.03] select-none pointer-events-none">
                01
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-500/10">
                <svg
                  className="h-5 w-5 text-amber-400"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth={1.5}
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15a2.25 2.25 0 0 1 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25ZM6.75 12h.008v.008H6.75V12Zm0 3h.008v.008H6.75V15Zm0 3h.008v.008H6.75V18Z"
                  />
                </svg>
              </div>
              <h3 className="mt-4 text-lg font-semibold">실전 문제 풀이</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">
                최신 출제 경향을 반영한 과목별 문제를 풀어보세요. 1과목 데이터
                모델링, 2과목 SQL 활용 전 범위를 다룹니다.
              </p>
            </div>
          </ScrollReveal>

          {/* Feature 2 */}
          <ScrollReveal delay={2}>
            <div className="group relative rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:-translate-y-1.5 hover:border-violet-500/30 hover:shadow-[0_0_24px_var(--accent-glow)] lg:-translate-y-2">
              <span className="absolute top-4 right-4 text-[64px] font-bold leading-none text-foreground/[0.03] select-none pointer-events-none">
                02
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-violet-500/10">
                <svg
                  className="h-5 w-5 text-violet-400"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth={1.5}
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z"
                  />
                </svg>
              </div>
              <h3 className="mt-4 text-lg font-semibold">오답 분석</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">
                틀린 문제를 자동으로 모아 취약 영역을 분석합니다. 해설과 함께
                복습하세요.
              </p>
            </div>
          </ScrollReveal>

          {/* Feature 3 */}
          <ScrollReveal delay={3}>
            <div className="group relative rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:-translate-y-1.5 hover:border-amber-500/30 hover:shadow-[0_0_24px_var(--glow)]">
              <span className="absolute top-4 right-4 text-[64px] font-bold leading-none text-foreground/[0.03] select-none pointer-events-none">
                03
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-amber-500/10 to-violet-500/10">
                <svg
                  className="h-5 w-5 text-amber-400"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth={1.5}
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                  />
                </svg>
              </div>
              <h3 className="mt-4 text-lg font-semibold">풀이 기록 관리</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">
                회차별 점수 추이와 풀이 시간을 기록합니다. 목표 점수까지 얼마나
                남았는지 한눈에 확인하세요.
              </p>
            </div>
          </ScrollReveal>
        </div>
      </section>

      {/* Preview */}
      <section
        id="preview"
        className="relative border-t border-border bg-surface/50 py-24"
      >
        {/* Spotlight background */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,var(--accent-glow),transparent_60%)] pointer-events-none" />

        <div className="relative mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <ScrollReveal>
            <h2 className="text-center text-2xl font-bold sm:text-3xl">
              문제 미리보기
            </h2>
            <p className="mt-3 text-center text-muted">
              실제 시험과 유사한 문제를 미리 풀어보세요
            </p>
          </ScrollReveal>

          <ScrollReveal delay={1}>
            <div className="mx-auto mt-12 max-w-3xl rounded-xl border border-border bg-surface p-6 sm:p-8">
              <div className="flex items-center gap-2 text-sm text-muted">
                <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
                  2과목
                </span>
                <span>SQL 활용</span>
              </div>

              <p className="mt-4 font-medium leading-relaxed">
                다음 SQL의 실행 결과로 올바른 것은?
              </p>

              {/* Code block with window chrome */}
              <div className="mt-4 rounded-lg bg-zinc-900 overflow-hidden border border-zinc-800">
                {/* Window chrome bar */}
                <div className="flex items-center gap-2 px-4 py-2.5 border-b border-zinc-800">
                  <span className="h-2.5 w-2.5 rounded-full bg-red-500/60" />
                  <span className="h-2.5 w-2.5 rounded-full bg-yellow-500/60" />
                  <span className="h-2.5 w-2.5 rounded-full bg-green-500/60" />
                  <span className="ml-2 text-[11px] text-zinc-500 font-mono">SQL Query</span>
                </div>
                <pre className="overflow-x-auto p-4 text-sm leading-relaxed text-zinc-300 font-mono">
                  <code>{`SELECT COUNT(*) AS CNT,
       SUM(CASE WHEN salary > 3000
                THEN 1 ELSE 0 END) AS HIGH_SAL
FROM   employee
WHERE  dept_id = 10;`}</code>
                  <span className="cursor-blink text-amber-400">|</span>
                </pre>
              </div>

              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <caption className="mb-2 text-left text-xs text-muted border-l-2 border-amber-500/50 pl-2">
                    [EMPLOYEE 테이블]
                  </caption>
                  <thead>
                    <tr className="border-b border-zinc-700 text-muted">
                      <th className="px-3 py-2 font-medium">EMP_ID</th>
                      <th className="px-3 py-2 font-medium">DEPT_ID</th>
                      <th className="px-3 py-2 font-medium">SALARY</th>
                    </tr>
                  </thead>
                  <tbody className="font-mono text-zinc-300">
                    <tr className="border-b border-zinc-800">
                      <td className="px-3 py-2">101</td>
                      <td className="px-3 py-2">10</td>
                      <td className="px-3 py-2">2500</td>
                    </tr>
                    <tr className="border-b border-zinc-800 bg-zinc-800/50">
                      <td className="px-3 py-2">102</td>
                      <td className="px-3 py-2">10</td>
                      <td className="px-3 py-2">3500</td>
                    </tr>
                    <tr className="border-b border-zinc-800">
                      <td className="px-3 py-2">103</td>
                      <td className="px-3 py-2">10</td>
                      <td className="px-3 py-2">4000</td>
                    </tr>
                    <tr className="bg-zinc-800/50">
                      <td className="px-3 py-2">104</td>
                      <td className="px-3 py-2">20</td>
                      <td className="px-3 py-2">3000</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <ul className="mt-6 space-y-3 text-sm">
                <li className="answer-option flex items-center gap-3 rounded-lg border border-border px-4 py-3 transition-all duration-300 hover:border-amber-500/40 hover:bg-amber-500/5 cursor-pointer">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-border text-xs text-muted transition-colors group-hover:border-amber-500/40">
                    1
                  </span>
                  <span>CNT: 3, HIGH_SAL: 2</span>
                </li>
                <li className="answer-option flex items-center gap-3 rounded-lg border border-border px-4 py-3 transition-all duration-300 hover:border-amber-500/40 hover:bg-amber-500/5 cursor-pointer">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-border text-xs text-muted">
                    2
                  </span>
                  <span>CNT: 4, HIGH_SAL: 2</span>
                </li>
                <li className="answer-option flex items-center gap-3 rounded-lg border border-border px-4 py-3 transition-all duration-300 hover:border-amber-500/40 hover:bg-amber-500/5 cursor-pointer">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-border text-xs text-muted">
                    3
                  </span>
                  <span>CNT: 3, HIGH_SAL: 3</span>
                </li>
                <li className="answer-option flex items-center gap-3 rounded-lg border border-border px-4 py-3 transition-all duration-300 hover:border-amber-500/40 hover:bg-amber-500/5 cursor-pointer">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-border text-xs text-muted">
                    4
                  </span>
                  <span>CNT: 4, HIGH_SAL: 3</span>
                </li>
              </ul>

              <details className="mt-6 rounded-lg border border-border px-4 py-3 text-sm">
                <summary className="cursor-pointer font-medium text-amber-400 flex items-center gap-2">
                  <svg className="chevron-icon h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                  정답 보기
                </summary>
                <p className="mt-3 leading-relaxed text-muted">
                  <strong className="text-foreground">정답: 1번</strong> — WHERE
                  dept_id = 10 조건으로 3건이 조회되며, salary &gt; 3000인 행은
                  102(3500)와 103(4000)으로 2건입니다.
                </p>
              </details>
            </div>
          </ScrollReveal>
        </div>
      </section>

      {/* CTA */}
      <section className="relative mx-auto max-w-5xl px-4 py-24 text-center sm:px-6 lg:px-8">
        {/* Shimmer divider */}
        <div className="shimmer-line mx-auto mb-16 max-w-md" />

        <ScrollReveal>
          <h2 className="text-2xl font-bold sm:text-3xl">
            지금 바로 시작하세요
          </h2>
          <p className="mt-3 text-muted">
            회원가입 없이 바로 문제를 풀어볼 수 있습니다.
          </p>
          <a
            href="/solve"
            className="btn-glow mt-8 inline-flex items-center rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-zinc-900 transition-all duration-300 hover:bg-primary-hover hover:scale-[1.03]"
          >
            문제 풀러 가기
          </a>
        </ScrollReveal>
      </section>

      {/* Footer */}
      <footer className="footer-gradient-border py-8 text-center text-sm text-muted">
        <p>&copy; 2025 SQLD Pass. All rights reserved.</p>
      </footer>
    </main>
  );
}
