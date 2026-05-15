import { redirect } from "next/navigation";

export default function EngineerWrittenPastExamsRedirect(): never {
  redirect("/past-exams?cert=ENGINEER_WRITTEN");
}
