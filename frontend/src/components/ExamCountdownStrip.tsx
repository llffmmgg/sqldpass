"use client";

import { useEffect, useMemo, useState } from "react";
import { EXAM_CERTS, pickUpcoming, diffDays } from "@/lib/examSchedules";

export default function ExamCountdownStrip() {
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
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

  return (
    <div className="mx-auto grid max-w-3xl grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
      {items.map((item) => {
        if (item.isAlwaysOpen) {
          return (
            <div
              key={item.cert.id}
              className="flex flex-col items-center rounded-xl border border-border/60 bg-surface/60 px-3 py-2.5 text-center"
            >
              <span className={`text-[10px] font-semibold uppercase tracking-wide ${item.cert.colorClass}`}>
                {item.cert.name}
              </span>
              <span className="mt-1 text-sm font-bold text-muted">상시</span>
              <span className="text-[10px] text-muted/70">수시 응시</span>
            </div>
          );
        }

        const { cert, upcoming, days } = item;
        const isOngoing = days! < 0; // 이미 시작했지만 기간 내라 upcoming으로 유지된 경우
        const isToday = days === 0;
        const isUrgent = days! > 0 && days! <= 7;

        const start = new Date(upcoming!.date + "T00:00:00+09:00");
        const startLabel = `${start.getMonth() + 1}.${start.getDate()}`;
        const end = upcoming!.endDate
          ? new Date(upcoming!.endDate + "T00:00:00+09:00")
          : null;
        const dateLabel = end
          ? `${startLabel}~${end.getMonth() + 1}.${end.getDate()}`
          : startLabel;

        return (
          <div
            key={cert.id}
            className={`flex flex-col items-center rounded-xl border ${cert.borderClass} bg-surface px-3 py-2.5 text-center ${
              isOngoing || isToday || isUrgent ? "shadow-[0_0_16px_var(--accent-glow)]" : ""
            }`}
          >
            <span className={`text-[10px] font-semibold uppercase tracking-wide ${cert.colorClass}`}>
              {cert.name}
            </span>
            <span
              className={`mt-1 text-lg font-bold tabular-nums leading-tight ${
                isOngoing
                  ? "text-accent"
                  : isToday
                    ? "text-accent"
                    : isUrgent
                      ? "text-primary"
                      : "text-foreground"
              }`}
            >
              {isOngoing ? "진행중" : isToday ? "D-DAY" : `D-${days}`}
            </span>
            <span className="text-[10px] text-muted">{dateLabel} · {upcoming!.label}</span>
          </div>
        );
      })}
    </div>
  );
}
