import type { HTMLAttributes } from "react";
import { cn } from "./cn";
import { CERT_TOKENS, type CertKey } from "@/lib/cert-tokens";

type Variant = "default" | "elevated" | "interactive";
type Padding = "none" | "sm" | "md" | "lg";

const variantClass: Record<Variant, string> = {
  default: "border border-border bg-surface",
  elevated: "border border-border bg-surface shadow-[var(--shadow-md)]",
  interactive:
    "border border-border bg-surface transition-all hover:border-border-strong hover:shadow-[var(--shadow-md)] hover:-translate-y-0.5",
};

const paddingClass: Record<Padding, string> = {
  none: "",
  sm: "p-4",
  md: "p-6",
  lg: "p-8",
};

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: Variant;
  padding?: Padding;
  accent?: CertKey;
  /** emerald glow on hover (use sparingly — primary hero cards only) */
  glow?: boolean;
}

function CardBase({
  variant = "default",
  padding = "md",
  accent,
  glow = false,
  className,
  children,
  ...props
}: CardProps) {
  const accentBorder = accent ? CERT_TOKENS[accent].tailwind.borderHover : null;
  const accentGlow = accent && glow ? CERT_TOKENS[accent].tailwind.glow : null;
  return (
    <div
      {...props}
      className={cn(
        "rounded-xl",
        variantClass[variant],
        paddingClass[padding],
        accentBorder,
        glow && !accent ? "hover:shadow-[0_0_24px_var(--glow)]" : null,
        accentGlow,
        className,
      )}
    >
      {children}
    </div>
  );
}

function CardHeader({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div {...props} className={cn("flex items-start justify-between gap-4", className)}>
      {children}
    </div>
  );
}

function CardTitle({ className, children, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3 {...props} className={cn("text-base font-semibold text-text", className)}>
      {children}
    </h3>
  );
}

function CardDescription({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p {...props} className={cn("text-sm leading-relaxed text-text-muted", className)}>
      {children}
    </p>
  );
}

function CardFooter({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      {...props}
      className={cn("mt-4 flex items-center justify-between gap-3", className)}
    >
      {children}
    </div>
  );
}

export const Card = Object.assign(CardBase, {
  Header: CardHeader,
  Title: CardTitle,
  Description: CardDescription,
  Footer: CardFooter,
});
