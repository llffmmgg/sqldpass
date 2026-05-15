import { redirect } from "next/navigation";

export default function EngineerPastExamsRedirect(): never {
  redirect("/past-exams?cert=ENGINEER_PRACTICAL");
}
