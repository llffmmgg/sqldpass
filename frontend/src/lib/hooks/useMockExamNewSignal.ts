"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 sessionStorage 캐시 로드/네트워크 응답을 setState 로 동기화하는 자연스러운 패턴 */

import { useEffect, useState } from "react";

import { isLoggedIn } from "@/lib/auth";
import type { CertKey } from "@/lib/cert-tokens";
import { isExamNew, countNewExams } from "@/lib/mockExamNew";
import { getMockExams, type MockExamSummary } from "@/lib/mockExamApi";
import { getPublicMockExams } from "@/lib/publicApi";

const CACHE_KEY = "mockexam:new-signal:v1";
const TTL_MS = 5 * 60 * 1000;

interface CachedPayload {
  fetchedAt: number;
  exams: Pick<
    MockExamSummary,
    "id" | "examType" | "createdAt" | "publishedAt" | "pastExamLinkedAt"
  >[];
}

interface Signal {
  newCount: number;
  hasNewByCert: (cert: CertKey) => boolean;
  loaded: boolean;
}

/**
 * NavBar 같은 전역 위치에서 모의고사 NEW 신호를 가져오는 훅.
 * sessionStorage 5분 캐시 — 페이지 이동마다 재호출하지 않음.
 */
export function useMockExamNewSignal(): Signal {
  const [exams, setExams] = useState<MockExamSummary[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const cached = readCache();
    if (cached) {
      setExams(cached.exams as MockExamSummary[]);
      setLoaded(true);
      return;
    }
    const fetcher = isLoggedIn() ? getMockExams() : getPublicMockExams();
    fetcher
      .then((list) => {
        writeCache(list);
        setExams(list);
        setLoaded(true);
      })
      .catch(() => {
        setLoaded(true);
      });
  }, []);

  const newCount = countNewExams(exams);
  const hasNewByCert = (cert: CertKey) =>
    exams.some((e) => e.examType === cert && isExamNew(e));

  return { newCount, hasNewByCert, loaded };
}

function readCache(): CachedPayload | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.sessionStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CachedPayload;
    if (Date.now() - parsed.fetchedAt > TTL_MS) return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeCache(list: MockExamSummary[]): void {
  if (typeof window === "undefined") return;
  try {
    const slim: CachedPayload = {
      fetchedAt: Date.now(),
      exams: list.map((e) => ({
        id: e.id,
        examType: e.examType,
        createdAt: e.createdAt,
        publishedAt: e.publishedAt,
        pastExamLinkedAt: e.pastExamLinkedAt,
      })),
    };
    window.sessionStorage.setItem(CACHE_KEY, JSON.stringify(slim));
  } catch {
    // 스토리지 차단/쿼터 초과 시 무시 — 캐시 없이도 정상 동작
  }
}
