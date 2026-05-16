"use client";

import { useEffect, useMemo, useState } from "react";
import { EXAM_CERTS, pickUpcoming, diffDays } from "@/lib/examSchedules";

export default function ExamCountdownStrip() {
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
    // 외부(Date) sync — SSR에서는 null, mount 직후 1회 동기화 후 60초 간격 갱신.
    // hydration mismatch 회피를 위해 첫 mount까지는 null로 두고 여기서만 setState.
    // eslint-disable-next-line react-hooks/set-state-in-effect -- 외부 Date sync, mount 1회 + setInterval 콜백
    setNow(new Date());
    const t = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(t);
  }, []);

  const items = useMemo(() => {
    if (!now) return [];
    return EXAM_CERTS.map((cert) => {
      if (cert.isAlwaysOpen) {
        return { cert, upcoming: null, days: null, isAlwaysOpen: true as const };
      }
      const upcoming = pickUpcoming(cert.schedules, now);
      if (!upcoming) return null;
      const target = new Date(upcoming.date + "T00:00:00+09:00");
      const days = diffDays(target, now);
      return { cert, upcoming, days, isAlwaysOpen: false as const };
    }).filter((x): x is NonNullable<typeof x> => x !== null);
  }, [now]);

  if (!now || items.length === 0) return null;

  // Supabase 트러스트바 스타일 — 가로 무한 스크롤. 아이템 2회 렌더 + translateX(-50%) 로
  // 끊김 없는 루프. 좌우 끝은 마스크 그라데이션으로 페이드. 호버 시 일시정지.
  const loopItems = [...items, ...items];

  return (
    <div
      className="mx-auto max-w-4xl overflow-hidden"
      style={{
        maskImage:
          "linear-gradient(to right, transparent, black 6%, black 94%, transparent)",
        WebkitMaskImage:
          "linear-gradient(to right, transparent, black 6%, black 94%, transparent)",
      }}
    >
      <div className="cert-marquee flex w-max items-start gap-x-9 sm:gap-x-12 md:gap-x-14">
        {loopItems.map((item, idx) => {
          const key = `${item.cert.id}-${idx}`;

          if (item.isAlwaysOpen) {
            return (
              <div key={key} className="flex flex-col items-center leading-tight">
                <span className="whitespace-nowrap text-[11px] font-semibold text-text-subtle sm:text-xs">
                  {item.cert.name}
                </span>
                <span className="mt-1 text-sm font-bold tabular-nums text-text-muted sm:text-base">
                  상시
                </span>
                <span className="mt-0.5 whitespace-nowrap text-[10px] text-text-subtle sm:text-[11px]">
                  수시 응시
                </span>
              </div>
            );
          }

          const { cert, upcoming, days } = item;
          const isOngoing = days! < 0;
          const isToday = days === 0;
          const isUrgent = days! > 0 && days! <= 7;
          const dayLabel = isOngoing ? "진행중" : isToday ? "D-DAY" : `D-${days}`;
          const dayClass = isOngoing || isToday || isUrgent ? "text-primary" : "text-text";

          const start = new Date(upcoming!.date + "T00:00:00+09:00");
          const startLabel = `${start.getMonth() + 1}.${start.getDate()}`;
          const end = upcoming!.endDate
            ? new Date(upcoming!.endDate + "T00:00:00+09:00")
            : null;
          const dateLabel = end
            ? `${startLabel}~${end.getMonth() + 1}.${end.getDate()}`
            : startLabel;

          return (
            <div key={key} className="flex flex-col items-center leading-tight">
              <span
                className="whitespace-nowrap text-[11px] font-semibold text-text-subtle sm:text-xs"
                aria-hidden={idx >= items.length}
              >
                {cert.name}
              </span>
              <span className={`mt-1 text-sm font-bold tabular-nums sm:text-base ${dayClass}`}>
                {dayLabel}
              </span>
              <span className="mt-0.5 whitespace-nowrap text-[10px] text-text-subtle sm:text-[11px]">
                {dateLabel} · {upcoming!.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
