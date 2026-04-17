import type { HTMLAttributes } from "react";
import { cn } from "./cn";

type Tone = "default" | "muted";

export interface SectionProps extends HTMLAttributes<HTMLElement> {
  tone?: Tone;
  /** vertical rhythm tightness. "compact" → py-16 md:py-20, "normal" → py-24 md:py-32 */
  spacing?: "compact" | "normal";
}

export function Section({
  tone = "default",
  spacing = "normal",
  className,
  children,
  ...props
}: SectionProps) {
  return (
    <section
      {...props}
      className={cn(
        "relative",
        tone === "muted" ? "bg-bg-elevated" : null,
        spacing === "compact" ? "py-16 md:py-20" : "py-24 md:py-32",
        className,
      )}
    >
      {children}
    </section>
  );
}
