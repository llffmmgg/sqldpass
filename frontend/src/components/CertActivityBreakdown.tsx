"use client";

import { useEffect, useState } from "react";

import { Card } from "@/components/ui";
import { CERT_TOKENS, type CertKey } from "@/lib/cert-tokens";
import {
  getPublicCertActivity,
  type PublicActivityBucket,
  type PublicCertActivity,
} from "@/lib/publicApi";

const SLUG_TO_CERT_KEY: Record<string, CertKey> = {
  sqld: "SQLD",
  engineer: "ENGINEER_PRACTICAL",
  "engineer-written": "ENGINEER_WRITTEN",
  "computer-literacy-1": "COMPUTER_LITERACY_1",
  "computer-literacy-2": "COMPUTER_LITERACY_2",
  adsp: "ADSP",
};

function fmt(n: number): string {
  return n.toLocaleString("ko-KR");
}

function totalsAcrossBuckets(items: PublicCertActivity["items"]) {
  let totalSolves = 0;
  let totalQuestions = 0;
  let totalUnique = 0;
  let todaySolves = 0;
  let todayQuestions = 0;
  let todayUnique = 0;
  for (const it of items) {
    for (const b of [it.mockExam, it.pastExam] as PublicActivityBucket[]) {
      totalSolves += b.totalSolves;
      totalQuestions += b.totalQuestions;
      totalUnique += b.uniqueMembers;
      todaySolves += b.todaySolves;
      todayQuestions += b.todayQuestions;
      todayUnique += b.todayUniqueMembers;
    }
  }
  return {
    totalSolves,
    totalQuestions,
    totalUnique,
    todaySolves,
    todayQuestions,
    todayUnique,
  };
}

export default function CertActivityBreakdown() {
  const [data, setData] = useState<PublicCertActivity | null>(null);
  const [errored, setErrored] = useState(false);

  useEffect(() => {
    getPublicCertActivity()
      .then(setData)
      .catch(() => setErrored(true));
  }, []);

  if (errored) return null;

  const items = data?.items ?? [];
  const totals = totalsAcrossBuckets(items);

  return (
    <div>
      {/* 상단 합계 라인 — 6개 자격증 + 두 종류 합산 */}
      <div className="mx-auto max-w-3xl rounded-xl border border-border bg-surface/60 px-5 py-4">
        <p className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">
          전체 누적 / 오늘
        </p>
        <div className="mt-2 flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2 text-sm">
          <div className="text-text-muted">
            <span className="font-semibold tabular-nums text-foreground">
              {data ? fmt(totals.totalUnique) : "--"}
            </span>
            명 응시 ·{" "}
            <span className="font-semibold tabular-nums text-foreground">
              {data ? fmt(totals.totalSolves) : "--"}
            </span>
            회 ·{" "}
            <span className="font-semibold tabular-nums text-foreground">
              {data ? fmt(totals.totalQuestions) : "--"}
            </span>
            문항 풀이
          </div>
          <div className="text-text-muted">
            오늘{" "}
            <span className="font-semibold tabular-nums text-foreground">
              {data ? fmt(totals.todayUnique) : "--"}
            </span>
            명 ·{" "}
            <span className="font-semibold tabular-nums text-foreground">
              {data ? fmt(totals.todaySolves) : "--"}
            </span>
            회 ·{" "}
            <span className="font-semibold tabular-nums text-foreground">
              {data ? fmt(totals.todayQuestions) : "--"}
            </span>
            문항
          </div>
        </div>
      </div>

      {/* 자격증별 카드 */}
      <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        {items.map((item) => {
          const certKey = SLUG_TO_CERT_KEY[item.certSlug];
          const token = certKey ? CERT_TOKENS[certKey] : null;
          return (
            <Card key={item.certSlug} padding="md" className="h-full">
              <div className="flex items-center gap-2">
                <span
                  className={`inline-block h-2 w-2 rounded-full ${
                    token?.tailwind.dot ?? "bg-text-muted"
                  }`}
                />
                <h3 className="text-sm font-semibold tracking-tight">
                  {token?.label ?? item.certName}
                </h3>
                <span className="ml-auto text-[10px] text-text-subtle">
                  오늘{" "}
                  <span className="font-semibold tabular-nums text-text-muted">
                    {fmt(
                      item.mockExam.todayUniqueMembers +
                        item.pastExam.todayUniqueMembers,
                    )}
                  </span>
                  명
                </span>
              </div>

              <CertBucketRow
                label="모의고사"
                bucket={item.mockExam}
                accent={token?.tailwind.text ?? "text-text"}
              />
              <CertBucketRow
                label="기출 복원"
                bucket={item.pastExam}
                accent={token?.tailwind.text ?? "text-text"}
              />
            </Card>
          );
        })}
      </div>
    </div>
  );
}

function CertBucketRow({
  label,
  bucket,
  accent,
}: {
  label: string;
  bucket: PublicActivityBucket;
  accent: string;
}) {
  return (
    <div className="mt-3 rounded-md border border-border bg-bg/40 px-3 py-2">
      <div className="flex items-center justify-between text-[11px] font-medium text-text-muted">
        <span className={accent}>{label}</span>
        <span className="tabular-nums">
          오늘 {fmt(bucket.todaySolves)}회 · {fmt(bucket.todayQuestions)}문항
        </span>
      </div>
      <div className="mt-1 flex items-center justify-between text-xs text-text-muted">
        <span>
          누적{" "}
          <span className="font-semibold tabular-nums text-foreground">
            {fmt(bucket.uniqueMembers)}
          </span>
          명
        </span>
        <span className="tabular-nums">
          {fmt(bucket.totalSolves)}회 · {fmt(bucket.totalQuestions)}문항
        </span>
      </div>
    </div>
  );
}
