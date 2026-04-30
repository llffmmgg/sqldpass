"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import MascotImage, { type MascotPose } from "./MascotImage";

interface MascotEmptyProps {
  pose: MascotPose;
  title: string;
  description: ReactNode;
  primaryCta?: { href: string; label: string };
  secondaryCta?: { href: string; label: string };
  hint?: ReactNode;
  className?: string;
}

export default function MascotEmpty({
  pose,
  title,
  description,
  primaryCta,
  secondaryCta,
  hint,
  className = "",
}: MascotEmptyProps) {
  return (
    <div
      className={`flex flex-col items-center px-6 py-16 text-center ${className}`}
    >
      <MascotImage pose={pose} size={140} />
      <h3 className="mt-5 text-xl font-bold tracking-tight sm:text-2xl">
        {title}
      </h3>
      <p className="mt-2 max-w-md text-sm leading-relaxed text-text-muted sm:text-base">
        {description}
      </p>

      {(primaryCta || secondaryCta) && (
        <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
          {primaryCta && (
            <Link
              href={primaryCta.href}
              className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-fg transition-colors hover:bg-primary-hover"
            >
              {primaryCta.label}
            </Link>
          )}
          {secondaryCta && (
            <Link
              href={secondaryCta.href}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-5 py-2.5 text-sm font-medium text-text transition-colors hover:border-border-strong"
            >
              {secondaryCta.label}
            </Link>
          )}
        </div>
      )}

      {hint && (
        <div className="mt-8 w-full max-w-sm rounded-lg border border-border bg-surface px-4 py-3 text-left">
          <p className="mb-1 text-[11px] font-bold uppercase tracking-wider text-primary">
            TIP
          </p>
          <div className="text-sm leading-relaxed text-text-muted">{hint}</div>
        </div>
      )}
    </div>
  );
}
