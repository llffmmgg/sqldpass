import type { HTMLAttributes } from "react";
import { cn } from "./cn";

type Size = "narrow" | "default" | "wide";

const sizeClass: Record<Size, string> = {
  narrow: "max-w-3xl",
  default: "max-w-5xl",
  wide: "max-w-6xl",
};

export interface ContainerProps extends HTMLAttributes<HTMLDivElement> {
  size?: Size;
}

export function Container({
  size = "default",
  className,
  children,
  ...props
}: ContainerProps) {
  return (
    <div
      {...props}
      className={cn(
        "mx-auto w-full px-4 sm:px-6 lg:px-8",
        sizeClass[size],
        className,
      )}
    >
      {children}
    </div>
  );
}
