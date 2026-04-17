import type { HTMLAttributes } from "react";
import { cn } from "./cn";
import { CERT_TOKENS, type CertKey } from "@/lib/cert-tokens";

type Variant = "solid" | "soft" | "outline";
type Tone = "neutral" | "primary" | "success" | "warning" | "danger" | "info";
type Size = "xs" | "sm";

const toneStyles: Record<Tone, { solid: string; soft: string; outline: string; dot: string }> = {
  neutral: {
    solid: "bg-surface-hover text-text border border-border",
    soft: "bg-surface text-text-muted border border-border",
    outline: "border border-border text-text-muted",
    dot: "bg-text-muted",
  },
  primary: {
    solid: "bg-primary text-[var(--primary-fg)]",
    soft: "bg-primary/10 text-primary border border-primary/30",
    outline: "border border-primary/40 text-primary",
    dot: "bg-primary",
  },
  success: {
    solid: "bg-success text-white",
    soft: "bg-success/10 text-success border border-success/30",
    outline: "border border-success/40 text-success",
    dot: "bg-success",
  },
  warning: {
    solid: "bg-warning text-white",
    soft: "bg-warning/10 text-warning border border-warning/30",
    outline: "border border-warning/40 text-warning",
    dot: "bg-warning",
  },
  danger: {
    solid: "bg-danger text-white",
    soft: "bg-danger/10 text-danger border border-danger/30",
    outline: "border border-danger/40 text-danger",
    dot: "bg-danger",
  },
  info: {
    solid: "bg-info text-white",
    soft: "bg-info/10 text-info border border-info/30",
    outline: "border border-info/40 text-info",
    dot: "bg-info",
  },
};

const sizeClass: Record<Size, string> = {
  xs: "text-[10px] px-2 py-0.5 gap-1",
  sm: "text-xs px-2.5 py-0.5 gap-1.5",
};

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: Variant;
  tone?: Tone;
  cert?: CertKey;
  size?: Size;
  dot?: boolean;
}

export function Badge({
  variant = "soft",
  tone = "neutral",
  cert,
  size = "sm",
  dot = false,
  className,
  children,
  ...props
}: BadgeProps) {
  if (cert) {
    const t = CERT_TOKENS[cert].tailwind;
    const certClasses =
      variant === "solid"
        ? cn(t.bg, "text-white")
        : variant === "soft"
          ? cn(t.bgSoft, t.text, "border", t.border)
          : cn("border", t.border, t.text);
    return (
      <span
        {...props}
        className={cn(
          "inline-flex items-center rounded-full font-medium",
          sizeClass[size],
          certClasses,
          className,
        )}
      >
        {dot && <span className={cn("h-1.5 w-1.5 rounded-full", t.dot)} />}
        {children}
      </span>
    );
  }
  const t = toneStyles[tone];
  const classes =
    variant === "solid" ? t.solid : variant === "soft" ? t.soft : t.outline;
  return (
    <span
      {...props}
      className={cn(
        "inline-flex items-center rounded-full font-medium",
        sizeClass[size],
        classes,
        className,
      )}
    >
      {dot && <span className={cn("h-1.5 w-1.5 rounded-full", t.dot)} />}
      {children}
    </span>
  );
}
