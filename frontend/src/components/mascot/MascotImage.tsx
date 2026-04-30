"use client";

import Image from "next/image";

export type MascotPose = "guide" | "analyze" | "check" | "work";

const POSE_TO_SRC: Record<MascotPose, string> = {
  guide: "/logo/blog-mascot.webp",
  analyze: "/logo/dashboard-mascot.webp",
  check: "/logo/wrong-answer-mascot.webp",
  work: "/logo/logo.webp",
};

interface MascotImageProps {
  pose: MascotPose;
  size?: number;
  className?: string;
  alt?: string;
  priority?: boolean;
}

export default function MascotImage({
  pose,
  size = 120,
  className = "",
  alt = "",
  priority = false,
}: MascotImageProps) {
  return (
    <Image
      src={POSE_TO_SRC[pose]}
      alt={alt}
      width={size}
      height={size}
      className={className}
      priority={priority}
      aria-hidden={alt === "" ? true : undefined}
    />
  );
}
