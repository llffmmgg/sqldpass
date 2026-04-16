export type ExamSchedule = {
  label: string;
  /** 시험 시작일 (기간 시험인 경우) 또는 시험일 (단일일) — 'YYYY-MM-DD' */
  date: string;
  /** 기간 시험(정처기 CBT 등)인 경우 종료일 — 'YYYY-MM-DD'. 없으면 단일일. */
  endDate?: string;
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
    // 큐넷 CBT 기간 — 시작~종료일 사이 자유 응시
    schedules: [
      { label: "2026년 1회", date: "2026-04-18", endDate: "2026-05-06" },
      { label: "2026년 2회", date: "2026-07-18", endDate: "2026-08-05" },
      { label: "2026년 3회", date: "2026-10-24", endDate: "2026-11-13" },
    ],
  },
  {
    id: "engineer-written",
    name: "정처기 필기",
    colorClass: "text-purple-500",
    borderClass: "border-purple-500/30",
    bgClass: "bg-purple-500/15",
    // 큐넷 CBT 기간 — 시작~종료일 사이 자유 응시
    schedules: [
      { label: "2026년 1회", date: "2026-01-30", endDate: "2026-03-03" },
      { label: "2026년 2회", date: "2026-05-09", endDate: "2026-05-29" },
      { label: "2026년 3회", date: "2026-08-07", endDate: "2026-09-01" },
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

/**
 * 현재 날짜 기준 "진행 중 또는 다가올" 시험을 고른다.
 * 기간 시험(endDate 있음)의 경우 endDate가 지나기 전까지 해당 회차를 유지.
 */
export function pickUpcoming(
  schedules: ExamSchedule[],
  now: Date,
): ExamSchedule | null {
  const today = new Date(now);
  today.setHours(0, 0, 0, 0);
  for (const s of schedules) {
    const endStr = s.endDate ?? s.date;
    const end = new Date(endStr + "T23:59:59+09:00");
    if (end.getTime() >= today.getTime()) return s;
  }
  return null;
}

export function diffDays(target: Date, now: Date): number {
  const ms = target.getTime() - now.getTime();
  return Math.ceil(ms / (1000 * 60 * 60 * 24));
}
