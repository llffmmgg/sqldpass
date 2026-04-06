declare module "korean-random-words" {
  interface PhraseGenConfig {
    customNouns?: string[];
    customAdjectives?: string[];
    delimiter?: string;
    adjSuffix?: string;
  }

  export default class PhraseGen {
    constructor(config?: PhraseGenConfig);
    generatePhrase(): string;
  }
}
