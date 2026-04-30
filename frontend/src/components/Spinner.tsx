import MascotSpinner from "./mascot/MascotSpinner";

export default function Spinner({ message = "로딩 중..." }: { message?: string }) {
  return <MascotSpinner message={message} />;
}
