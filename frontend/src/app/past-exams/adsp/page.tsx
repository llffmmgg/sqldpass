import { redirect } from "next/navigation";

export default function AdspPastExamsRedirect(): never {
  redirect("/past-exams?cert=ADSP");
}
