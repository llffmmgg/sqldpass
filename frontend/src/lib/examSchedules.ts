export type ExamSchedule = {
  label: string;
  date: string; // 'YYYY-MM-DD'
};

export type ExamCert = {
  id: string;
  name: string;
  colorClass: string;
  borderClass: string;
  bgClass: string;
  schedules: ExamSchedule[];
  isAlwaysOpen?: boolean;
};

// 공식 사이트 확인 기준 2026년 시험 일정
// SQLD/ADsP: dataq.or.kr | 정처기: 큐넷 | 컴활: 대한상공회의소 (상시)
export const EXAM_CERTS: ExamCert[] = [
  {
    id: "sqld",
    name: "SQLD",
    colorClass: "text-primary",
    borderClass: "border-primary/30",
    bgClass: "bg-primary/15",
    schedules: [
      { label: "제60회", date: "2026-03-07" },
      { label: "제61회", date: "2026-05-31" },
      { label: "제62회", date: "2026-08-22" },
      { label: "제63회", date: "2026-11-14" },
    ],
  },
  {
    id: "engineer",
    name: "정처기 실기",
    colorClass: "text-accent",
    borderClass: "border-accent/30",
    bgClass: "bg-accent/15",
    schedules: [
      { label: "2026년 1회", date: "2026-04-18" },
      { label: "2026년 2회", date: "2026-07-18" },
      { label: "2026년 3회", date: "2026-10-24" },
    ],
  },
  {
    id: "engineer-written",
    name: "정처기 필기",
    colorClass: "text-purple-500",
    borderClass: "border-purple-500/30",
    bgClass: "bg-purple-500/15",
    schedules: [
      { label: "2026년 1회", date: "2026-01-30" },
      { label: "2026년 2회", date: "2026-05-09" },
      { label: "2026년 3회", date: "2026-08-07" },
    ],
  },
  {
    id: "adsp",
    name: "ADsP",
    colorClass: "text-teal-500",
    borderClass: "border-teal-500/30",
    bgClass: "bg-teal-500/15",
    schedules: [
      { label: "제48회", date: "2026-02-07" },
      { label: "제49회", date: "2026-05-17" },
      { label: "제50회", date: "2026-08-08" },
      { label: "제51회", date: "2026-10-31" },
    ],
  },
  {
    id: "computer-literacy-1",
    name: "컴활 1급",
    colorClass: "text-blue-600",
    borderClass: "border-blue-600/30",
    bgClass: "bg-blue-600/15",
    schedules: [],
    isAlwaysOpen: true,
  },
  {
    id: "computer-literacy-2",
    name: "컴활 2급",
    colorClass: "text-indigo-500",
    borderClass: "border-indigo-500/30",
    bgClass: "bg-indigo-500/15",
    schedules: [],
    isAlwaysOpen: true,
  },
];

export function pickUpcoming(
  schedules: ExamSchedule[],
  now: Date,
): ExamSchedule | null {
  const today = new Date(now);
  today.setHours(0, 0, 0, 0);
  for (const s of schedules) {
    const d = new Date(s.date + "T00:00:00+09:00");
    if (d.getTime() >= today.getTime()) return s;
  }
  return null;
}

export function diffDays(target: Date, now: Date): number {
  const ms = target.getTime() - now.getTime();
  return Math.ceil(ms / (1000 * 60 * 60 * 24));
}
