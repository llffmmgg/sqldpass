import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode, AnchorHTMLAttributes } from "react";
import { cn } from "./cn";

type Variant = "primary" | "secondary" | "ghost" | "outline" | "danger";
type Size = "sm" | "md" | "lg";

const variantClass: Record<Variant, string> = {
  primary:
    "bg-primary text-[var(--primary-fg)] hover:bg-primary-hover shadow-[0_1px_2px_rgba(0,0,0,0.08)]",
  secondary:
    "bg-surface text-text border border-border hover:bg-surface-hover hover:border-border-strong",
  ghost:
    "text-text-muted hover:text-text hover:bg-surface-hover",
  outline:
    "border border-border text-text hover:border-primary/40 hover:bg-primary-soft",
  danger:
    "bg-danger/10 text-danger border border-danger/30 hover:bg-danger/15",
};

const sizeClass: Record<Size, string> = {
  sm: "h-8 px-3 text-xs rounded-md gap-1.5",
  md: "h-9 px-4 text-sm rounded-lg gap-2",
  lg: "h-11 px-5 text-sm rounded-xl gap-2",
};

function base(variant: Variant, size: Size, glow: boolean, extra?: string) {
  return cn(
    "inline-flex items-center justify-center font-medium whitespace-nowrap transition-all",
    "disabled:cursor-not-allowed disabled:opacity-50",
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--primary-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--bg)]",
    variantClass[variant],
    sizeClass[size],
    glow && variant === "primary" && size === "lg" ? "btn-glow" : null,
    extra,
  );
}

function Spinner({ size }: { size: Size }) {
  const box = size === "lg" ? "h-4 w-4" : "h-3.5 w-3.5";
  return (
    <svg
      className={cn("animate-spin", box)}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeOpacity="0.25" strokeWidth="3" />
      <path d="M22 12a10 10 0 0 1-10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  glow?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
}

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  glow = false,
  leftIcon,
  rightIcon,
  disabled,
  className,
  children,
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      {...props}
      disabled={disabled || loading}
      className={base(variant, size, glow, className)}
    >
      {loading ? <Spinner size={size} /> : leftIcon}
      {children}
      {!loading && rightIcon}
    </button>
  );
}

export interface ButtonLinkProps
  extends Omit<AnchorHTMLAttributes<HTMLAnchorElement>, "href"> {
  href: string;
  variant?: Variant;
  size?: Size;
  glow?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  /** external link (use plain <a> + target). */
  external?: boolean;
}

export function ButtonLink({
  href,
  variant = "primary",
  size = "md",
  glow = false,
  leftIcon,
  rightIcon,
  external,
  className,
  children,
  ...props
}: ButtonLinkProps) {
  const classes = base(variant, size, glow, className);
  const content = (
    <>
      {leftIcon}
      {children}
      {rightIcon}
    </>
  );
  if (external) {
    return (
      <a
        href={href}
        className={classes}
        target="_blank"
        rel="noopener noreferrer"
        {...props}
        role="button"
      >
        {content}
      </a>
    );
  }
  return (
    <Link href={href} className={classes} {...props} role="button">
      {content}
    </Link>
  );
}
