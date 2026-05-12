"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

type ToastKind = "success" | "info" | "error";
type ToastItem = { id: number; message: string; kind: ToastKind };

type ToastContextValue = {
  show: (message: string, kind?: ToastKind) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

// Provider 미존재 시 useToast() 가 반환할 no-op — 모듈 단위 상수라 매 호출 같은 reference.
// (전엔 매 호출 새 객체였어서 useToast 결과를 useEffect deps 에 넣은 곳이 무한 재실행됨.)
const NOOP_TOAST: ToastContextValue = { show: () => {} };

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  return ctx ?? NOOP_TOAST;
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);

  const show = useCallback((message: string, kind: ToastKind = "info") => {
    const id = Date.now() + Math.random();
    setItems((prev) => [...prev, { id, message, kind }]);
    setTimeout(() => {
      setItems((prev) => prev.filter((it) => it.id !== id));
    }, 3000);
  }, []);

  // Provider value 를 매 렌더 새 객체 리터럴로 만들면 useToast 결과가 unstable.
  // useEffect deps 에 toast 가 들어간 곳(예: CheckoutClient redirectUrl 복귀 처리)이
  // 무한 재실행되는 사고를 피하기 위해 memoize.
  const value = useMemo<ToastContextValue>(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 top-4 z-[100] flex flex-col items-center gap-2 px-4"
      >
        {items.map((it) => (
          <ToastBubble key={it.id} message={it.message} kind={it.kind} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastBubble({ message, kind }: { message: string; kind: ToastKind }) {
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const t = requestAnimationFrame(() => setVisible(true));
    return () => cancelAnimationFrame(t);
  }, []);

  const iconTone =
    kind === "success"
      ? "bg-success/15 text-success"
      : kind === "error"
      ? "bg-danger/15 text-danger"
      : "bg-primary/15 text-primary";

  return (
    <div
      className={`pointer-events-auto flex max-w-md items-start gap-3 rounded-lg border border-border bg-surface px-4 py-3 text-sm font-medium text-text shadow-xl transition-all duration-300 ${
        visible ? "translate-y-0 opacity-100" : "-translate-y-2 opacity-0"
      }`}
      role="status"
    >
      <span
        className={`mt-px flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full ${iconTone}`}
        aria-hidden
      >
        <ToastIcon kind={kind} />
      </span>
      <span className="flex-1 leading-snug">{message}</span>
    </div>
  );
}

function ToastIcon({ kind }: { kind: ToastKind }) {
  if (kind === "success") {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2.5}
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-3.5 w-3.5"
      >
        <path d="M5 13l4 4L19 7" />
      </svg>
    );
  }
  if (kind === "error") {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2.5}
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-3.5 w-3.5"
      >
        <line x1="12" y1="8" x2="12" y2="13" />
        <circle cx="12" cy="16.5" r="0.5" fill="currentColor" stroke="none" />
      </svg>
    );
  }
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-3.5 w-3.5"
    >
      <line x1="12" y1="11" x2="12" y2="16" />
      <circle cx="12" cy="8" r="0.5" fill="currentColor" stroke="none" />
    </svg>
  );
}
