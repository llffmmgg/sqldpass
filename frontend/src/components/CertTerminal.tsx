"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";

/**
 * 자격증 선택 터미널 — 히어로 하단에 배치.
 * 1) 상단: `$ ./sqldpass --exam={activeCert} --mode=mock` 타이프라이터
 * 2) 하단: 터미널 프롬프트 스타일 chip selector
 *
 * 기존 font-mono + cursor-blink(globals.css) 재사용.
 */

type Cert = {
  id: "sqld" | "engineer";
  label: string;
  count: number;
};

const CERTS: Cert[] = [
  { id: "sqld", label: "SQLD", count: 50 },
  { id: "engineer", label: "정처기 실기", count: 20 },
];

function commandFor(id: Cert["id"]) {
  return `$ ./sqldpass --exam=${id} --mode=mock`;
}

// SSR에서는 false, 클라이언트 hydration 직후 true — useSyncExternalStore가 mismatch 없이 처리.
function useIsClient() {
  return useSyncExternalStore(
    () => () => {},
    () => true,
    () => false,
  );
}

// 타이프라이터 — `key={text}` 등으로 부모가 remount하면 typed 상태가 자연스럽게 ""로 초기화된다.
// setState는 setTimeout 콜백 내부에서만 호출되므로 set-state-in-effect 룰에 걸리지 않는다.
function Typewriter({ text }: { text: string }) {
  const [typed, setTyped] = useState("");
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    let i = 0;
    function tick() {
      i += 1;
      setTyped(text.slice(0, i));
      if (i < text.length) {
        timerRef.current = setTimeout(tick, 38);
      }
    }
    timerRef.current = setTimeout(tick, 120);
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [text]);

  return <>{typed}</>;
}

export default function CertTerminal() {
  const [active, setActive] = useState<Cert["id"]>("sqld");
  const isClient = useIsClient();

  const currentCert = CERTS.find((c) => c.id === active) ?? CERTS[0];
  const staticCmd = commandFor(active);

  return (
    <div className="mx-auto mt-10 w-full max-w-xl">
      {/* Terminal window */}
      <div className="rounded-lg border border-zinc-800/80 bg-zinc-950/70 backdrop-blur-sm shadow-[0_0_32px_rgba(245,158,11,0.06)]">
        {/* Chrome bar */}
        <div className="flex items-center gap-2 border-b border-zinc-800/80 px-3.5 py-2">
          <span className="h-2.5 w-2.5 rounded-full bg-red-500/60" />
          <span className="h-2.5 w-2.5 rounded-full bg-yellow-500/60" />
          <span className="h-2.5 w-2.5 rounded-full bg-green-500/60" />
          <span className="ml-2 font-mono text-[10px] uppercase tracking-wider text-zinc-600">
            ~/certs — {currentCert.label} · {currentCert.count}문항
          </span>
        </div>
        {/* Prompt line */}
        <div className="px-4 py-3 font-mono text-sm leading-relaxed">
          {/* SSR/hydration: 정적 문자열, 클라이언트 mount 후 active 마다 Typewriter 재실행 */}
          <span className="text-zinc-300">
            {isClient ? <Typewriter key={active} text={staticCmd} /> : staticCmd}
          </span>
          <span className="cursor-blink ml-0.5 text-amber-400">▍</span>
        </div>
      </div>

      {/* Chip selector */}
      <div className="mt-4 flex flex-wrap items-center justify-center gap-2 font-mono text-xs">
        <span className="text-muted/60">~/certs $</span>
        {CERTS.map((c) => {
          const isActive = c.id === active;
          return (
            <button
              key={c.id}
              onClick={() => setActive(c.id)}
              aria-pressed={isActive}
              className={`inline-flex items-center rounded-md border px-3 py-1.5 transition-all duration-200 ${
                isActive
                  ? "border-amber-400/50 bg-amber-500/10 text-amber-300 shadow-[0_0_12px_rgba(245,158,11,0.15)]"
                  : "border-border bg-transparent text-muted hover:border-violet-500/40 hover:text-foreground"
              }`}
            >
              {isActive && <span className="mr-1.5 text-amber-400">▸</span>}
              {c.label}
            </button>
          );
        })}
        <span className="inline-flex items-center rounded-md border border-dashed border-border/60 px-3 py-1.5 text-muted/60">
          + Soon
        </span>
      </div>
    </div>
  );
}
