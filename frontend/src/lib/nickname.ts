// korean-random-words 라이브러리 래퍼.
// 기본은 3단어 형식("뾰족하고-용감한-선인장")인데, 마지막 두 단어만 추출해서
// 짧은 형태("용감한 선인장")로 사용한다.

import PhraseGen from "korean-random-words";

const phraseGen = new PhraseGen();

/**
 * 짧은 한국어 닉네임 생성: "형용사한 명사" (예: "용감한 선인장")
 */
export function generateNickname(): string {
  const full = phraseGen.generatePhrase(); // "뾰족하고-용감한-선인장"
  const parts = full.split("-");
  // 마지막 두 단어 ("용감한", "선인장") 만 사용
  if (parts.length >= 2) {
    return parts.slice(-2).join(" ");
  }
  return full;
}
