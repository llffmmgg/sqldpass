"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

type ToastKind = "success" | "info" | "error";
type ToastItem = { id: number; message: string; kind: ToastKind };

type ToastContextValue = {
  show: (message: string, kind?: ToastKind) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    // Provider 없이 호출된 경우에도 크래시 없이 no-op
    return { show: () => {} };
  }
  return ctx;
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

  return (
    <ToastContext.Provider value={{ show }}>
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

  const color =
    kind === "success"
      ? "border-emerald-500/40 bg-emerald-500/15 text-emerald-200"
      : kind === "error"
      ? "border-rose-500/40 bg-rose-500/15 text-rose-200"
      : "border-primary/40 bg-primary/15 text-primary";

  return (
    <div
      className={`pointer-events-auto max-w-md rounded-lg border px-4 py-2.5 text-sm font-medium shadow-lg backdrop-blur transition-all duration-300 ${color} ${
        visible ? "translate-y-0 opacity-100" : "-translate-y-2 opacity-0"
      }`}
    >
      {message}
    </div>
  );
}
