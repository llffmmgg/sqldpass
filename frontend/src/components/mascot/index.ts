import MascotImage, { type MascotPose } from "./MascotImage";
export { MascotImage };
export type { MascotPose };
export { default as MascotEmpty } from "./MascotEmpty";
export { default as MascotSpinner } from "./MascotSpinner";

export function poseFromScore(score: number): MascotPose {
  if (score >= 90) return "guide";
  if (score >= 60) return "analyze";
  return "check";
}
