"use client";

import { isCapacitorApp } from "@/lib/platform";
import type { AnchorHTMLAttributes, MouseEvent, ReactNode } from "react";

type Props = Omit<AnchorHTMLAttributes<HTMLAnchorElement>, "href" | "target"> & {
  href: string;
  children: ReactNode;
};

export default function ExternalLink({ href, onClick, children, ...rest }: Props) {
  function handleClick(e: MouseEvent<HTMLAnchorElement>) {
    onClick?.(e);
    if (e.defaultPrevented) return;
    if (!isCapacitorApp()) return;
    const browser = typeof window !== "undefined"
      ? window.Capacitor?.Plugins?.Browser
      : undefined;
    if (!browser) return; // Plugin not installed yet — fall back to native anchor behaviour.
    e.preventDefault();
    void browser.open({ url: href });
  }

  return (
    <a href={href} target="_blank" rel="noopener noreferrer" onClick={handleClick} {...rest}>
      {children}
    </a>
  );
}
