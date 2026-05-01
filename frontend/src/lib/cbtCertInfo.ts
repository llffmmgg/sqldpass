import type { CertKey } from "@/lib/cert-tokens";

export interface CbtCertInfo {
  cert: CertKey;
  /** SEO 의 핵심. 제목·H1·메타에 자주 등장 */
  searchKeyword: string;
  /** "정처기 필기", "SQLD" 같은 짧은 검색어 그대로 */
  shortKeyword: string;
  /** 풀네임 (예: "정보처리기사 필기") */
  longKeyword: string;
  /** title tag 본문 */
  title: string;
  /** meta description (130~160자) */
  description: string;
  /** OG 이미지 alt 등에 쓰는 한 줄 요약 */
  oneLiner: string;
  /** keywords 배열 */
  keywords: string[];
  /** 시험 정보 표 */
  examInfo: {
    questions: string;
    duration: string;
    passLine: string;
    examType: string;
    organizer: string;
  };
  /** 출제 영역 / 과목 — 문자열 또는 항목 리스트 */
  subjects: { name: string; description: string }[];
  /** 학습 팁 (200자 내) */
  studyTip: string;
  /** FAQ */
  faqs: { q: string; a: string }[];
}

export const CBT_CERT_INFO: Record<CertKey, CbtCertInfo> = {
  SQLD: {
    cert: "SQLD",
    searchKeyword: "SQLD CBT",
    shortKeyword: "SQLD",
    longKeyword: "SQL 개발자(SQLD)",
    title: "SQLD CBT 무료 모의고사 | SQL 개발자 실전 타이머 | 문어CBT",
    description:
      "SQLD CBT 모의고사를 무료로. SQL 개발자 자격증 50문항 90분 실전 환경, 자동 채점·해설·오답 누적까지. 회차별 기출 복원과 AI 변형 문제로 합격선까지 빠르게.",
    oneLiner: "SQLD CBT 모의고사 무료 · 실전 타이머 · 자동 채점",
    keywords: [
      "SQLD CBT",
      "SQLD 모의고사",
      "SQLD 무료",
      "SQLD 기출",
      "SQLD 기출 CBT",
      "SQL 개발자 CBT",
      "SQL 개발자 모의고사",
      "SQLD 합격",
      "SQLD 50문항",
      "SQLD 데이터 모델링",
      "SQLD SQL 활용",
    ],
    examInfo: {
      questions: "50문항 (객관식 4지선다)",
      duration: "90분",
      passLine: "총점 60점 이상 / 과목별 40% 미만 과락",
      examType: "CBT (컴퓨터 시험)",
      organizer: "한국데이터산업진흥원(K-DATA)",
    },
    subjects: [
      {
        name: "1과목. 데이터 모델링의 이해",
        description:
          "엔터티·속성·관계, 정규화, 식별자, 본질·인조 식별자. 10문항 출제, 과락(40% 미만) 주의.",
      },
      {
        name: "2과목. SQL 기본 및 활용",
        description:
          "SELECT·JOIN·서브쿼리·집합 연산, 윈도우 함수, 계층형(CONNECT BY), 그룹 함수, NULL 처리, PL/SQL 기초까지. 40문항.",
      },
    ],
    studyTip:
      "SQLD CBT 는 1과목 과락만 피하면 2과목에서 합격선 도달이 충분합니다. 회차별 기출 복원을 시간 재서 풀어보고, 윈도우 함수·계층형 쿼리·NULL 함정을 따로 정리하세요.",
    faqs: [
      {
        q: "SQLD CBT 모의고사 무료인가요?",
        a: "네, 문어CBT 의 SQLD CBT 모의고사는 모두 무료입니다. Google 로그인 한 번이면 실전 타이머·자동 채점·오답 누적까지 사용할 수 있어요.",
      },
      {
        q: "SQLD 시험은 CBT 인가요 PBT 인가요?",
        a: "SQLD 는 2022년부터 CBT(컴퓨터 시험)로 전환되었습니다. 종이 OMR 이 아니라 컴퓨터 화면에서 답안을 표시하는 방식이라 CBT 환경 적응이 합격에 직결됩니다.",
      },
      {
        q: "기출 변형이라는 건 무엇인가요?",
        a: "원문을 그대로 복제하지 않고, 같은 개념과 난이도의 문제를 새로 구성합니다. 매번 응시할 때마다 새로운 문제가 출제되어 단순 암기가 아닌 실력으로 합격선 통과가 가능해요.",
      },
      {
        q: "몇 회 풀어야 합격권에 들 수 있나요?",
        a: "보통 SQLD CBT 모의고사 3~5회 + 1과목 데이터 모델링 별도 정리 + 윈도우 함수·계층형 정리이면 합격권에 들어옵니다.",
      },
    ],
  },
  ENGINEER_WRITTEN: {
    cert: "ENGINEER_WRITTEN",
    searchKeyword: "정처기 필기 CBT",
    shortKeyword: "정처기 필기",
    longKeyword: "정보처리기사 필기",
    title: "정처기 필기 CBT 무료 모의고사 | 정보처리기사 필기 실전 | 문어CBT",
    description:
      "정처기 필기 CBT 모의고사를 무료로. 정보처리기사 필기 5과목 100문항 실전 타이머, 자동 채점·해설·오답 누적. 회차별 기출 복원과 AI 변형 문제로 빠르게 합격선까지.",
    oneLiner: "정처기 필기 CBT 모의고사 무료 · 100문항 실전 타이머",
    keywords: [
      "정처기 필기 CBT",
      "정처기 필기 모의고사",
      "정처기 필기 무료",
      "정처기 필기 기출",
      "정보처리기사 필기 CBT",
      "정보처리기사 필기 모의고사",
      "정보처리기사 필기 기출",
      "정처기 CBT",
      "정처기 100문항",
    ],
    examInfo: {
      questions: "100문항 (객관식 4지선다)",
      duration: "150분 (2시간 30분)",
      passLine: "과목별 40% 이상 + 평균 60점 이상",
      examType: "CBT (컴퓨터 시험)",
      organizer: "한국산업인력공단(Q-Net)",
    },
    subjects: [
      { name: "1과목. 소프트웨어 설계", description: "요구사항 분석, UML, 디자인 패턴, 아키텍처 패턴 (20문항)" },
      { name: "2과목. 소프트웨어 개발", description: "데이터 입출력, 통합 구현, 테스트, 인터페이스 (20문항)" },
      { name: "3과목. 데이터베이스 구축", description: "SQL, 정규화, 트랜잭션, 회복·병행 제어 (20문항)" },
      { name: "4과목. 프로그래밍 언어 활용", description: "C·Java·Python 코드 분석, 운영체제, 네트워크 (20문항)" },
      { name: "5과목. 정보시스템 구축 관리", description: "보안, 신기술 동향, 소프트웨어 개발 방법론 (20문항)" },
    ],
    studyTip:
      "정처기 필기 CBT 는 5과목 100문항 4지선다입니다. 과목별 40% 과락 + 평균 60점이 핵심. 한 과목도 버릴 수 없으니 출제 빈도 높은 영역부터 회독하고, 4과목 코드 문제는 별도 시간 배분이 필요합니다.",
    faqs: [
      {
        q: "정처기 필기 CBT 모의고사 무료인가요?",
        a: "네, 문어CBT 의 정처기 필기 CBT 모의고사는 모두 무료입니다. 100문항 실전 타이머와 자동 채점, 과목별 정답률 분석까지 무료로 제공됩니다.",
      },
      {
        q: "정처기 필기는 언제부터 CBT 가 되었나요?",
        a: "정보처리기사 필기는 2022년 3회차부터 CBT 로 전환되었습니다. 종이 시험 환경과 다르므로 CBT 화면 적응 연습이 합격에 결정적입니다.",
      },
      {
        q: "비전공자도 합격할 수 있나요?",
        a: "가능합니다. 출제 영역이 정해져 있어 6~8주 집중 학습으로 합격선 진입 사례가 많아요. 기출 복원으로 출제 패턴을 익히는 게 가장 빠릅니다.",
      },
      {
        q: "코드 문제(C/Java/Python) 비중이 큰가요?",
        a: "4과목에 약 6~8문항 출제됩니다. 비중이 크진 않지만 풀이 시간이 길어져 시간 분배 전략이 필요해요. CBT 환경에서 미리 연습해두는 걸 권장합니다.",
      },
    ],
  },
  ENGINEER_PRACTICAL: {
    cert: "ENGINEER_PRACTICAL",
    searchKeyword: "정처기 실기 CBT",
    shortKeyword: "정처기 실기",
    longKeyword: "정보처리기사 실기",
    title: "정처기 실기 CBT 무료 모의고사 | 정보처리기사 실기 단답·약술 | 문어CBT",
    description:
      "정처기 실기 CBT 모의고사를 무료로. 정보처리기사 실기 단답형·약술형 출제 영역을 키워드 중심으로 정리하고, 회차별 기출 복원과 AI 변형 문제로 합격선 60점까지.",
    oneLiner: "정처기 실기 CBT 모의고사 무료 · 단답·약술 키워드 학습",
    keywords: [
      "정처기 실기 CBT",
      "정처기 실기 모의고사",
      "정처기 실기 무료",
      "정처기 실기 기출",
      "정처기 실기 키워드",
      "정보처리기사 실기 CBT",
      "정보처리기사 실기 모의고사",
      "정보처리기사 실기 기출",
    ],
    examInfo: {
      questions: "20문항 (단답형·약술형·코드 빈칸)",
      duration: "150분 (2시간 30분)",
      passLine: "총점 60점 이상",
      examType: "CBT (컴퓨터 시험)",
      organizer: "한국산업인력공단(Q-Net)",
    },
    subjects: [
      { name: "소프트웨어 설계·개발", description: "요구사항·UML·디자인 패턴·테스트" },
      { name: "데이터베이스", description: "SQL 작성·정규화·트랜잭션" },
      { name: "프로그래밍 언어", description: "C·Java·Python 코드 빈칸·결과 예측" },
      { name: "정보보안", description: "암호 알고리즘·접근 통제·공격 기법" },
      { name: "신기술 동향", description: "AI·클라우드·DevOps 등 약어·키워드" },
    ],
    studyTip:
      "정처기 실기 CBT 는 단답·약술형 비중이 큽니다. 자주 나오는 키워드(SQL, 보안, 디자인 패턴, 신기술 동향)를 정리하고, 출제 빈도 높은 챕터부터 회독하는 게 효율적입니다.",
    faqs: [
      {
        q: "정처기 실기 CBT 모의고사 무료인가요?",
        a: "네, 문어CBT 의 정처기 실기 CBT 모의고사는 모두 무료입니다. 단답·약술 키워드 채점과 모범답안·해설을 무료로 제공해요.",
      },
      {
        q: "정처기 실기는 CBT 인가요?",
        a: "네, 정보처리기사 실기는 2022년부터 CBT 로 전환되었습니다. 키보드로 답안을 입력하므로 한글 입력 속도와 키워드 정확도가 합격에 직결됩니다.",
      },
      {
        q: "실기는 단답형이 전부인가요?",
        a: "단답형이 가장 많지만 약술형(2~3줄)·코드 빈칸 채우기도 출제됩니다. 키워드 외에도 한 줄 설명을 함께 익혀두면 약술 대응이 됩니다.",
      },
      {
        q: "필기 합격 후 바로 실기 봐도 되나요?",
        a: "필기와 실기는 출제 영역은 비슷하지만 답안 형태가 다릅니다. 필기는 4지선다 인식, 실기는 키워드 직접 작성이라 별도 키워드 정리 학습이 필요해요.",
      },
    ],
  },
  COMPUTER_LITERACY_1: {
    cert: "COMPUTER_LITERACY_1",
    searchKeyword: "컴활 1급 CBT",
    shortKeyword: "컴활 1급",
    longKeyword: "컴퓨터활용능력 1급 필기",
    title: "컴활 1급 CBT 무료 모의고사 | 컴퓨터활용능력 1급 필기 실전 | 문어CBT",
    description:
      "컴활 1급 CBT 모의고사를 무료로. 컴퓨터활용능력 1급 필기 60문항 60분 실전 타이머, 컴퓨터 일반·스프레드시트·데이터베이스 자동 채점과 해설까지.",
    oneLiner: "컴활 1급 CBT 모의고사 무료 · 60문항 실전 타이머",
    keywords: [
      "컴활 1급 CBT",
      "컴활 1급 모의고사",
      "컴활 1급 무료",
      "컴활 1급 기출",
      "컴퓨터활용능력 1급 CBT",
      "컴퓨터활용능력 1급 모의고사",
      "컴활 필기 CBT",
      "컴활 매크로 CBT",
    ],
    examInfo: {
      questions: "60문항 (객관식 4지선다)",
      duration: "60분",
      passLine: "과목별 40% 이상 + 평균 60점 이상",
      examType: "CBT (컴퓨터 시험)",
      organizer: "대한상공회의소",
    },
    subjects: [
      { name: "1과목. 컴퓨터 일반", description: "운영체제·인터넷·정보보안·멀티미디어 (20문항)" },
      { name: "2과목. 스프레드시트 일반", description: "엑셀 함수·매크로(VBA)·차트·피벗 테이블 (20문항)" },
      { name: "3과목. 데이터베이스 일반", description: "Access·SQL·정규화·테이블 관계 (20문항)" },
    ],
    studyTip:
      "컴활 1급 CBT 는 매크로(VBA), 함수, 데이터베이스 정규화가 고정 단골 출제 영역입니다. 기출 복원 회독으로 패턴을 익히는 게 가장 빠르고, 2급보다 3과목 데이터베이스가 추가되어 학습량이 1.5배 정도 많습니다.",
    faqs: [
      {
        q: "컴활 1급 CBT 모의고사 무료인가요?",
        a: "네, 문어CBT 의 컴활 1급 CBT 모의고사는 모두 무료입니다. 60문항 실전 타이머와 과목별 정답률 분석까지 무료로 제공됩니다.",
      },
      {
        q: "컴활 1급 필기는 CBT 인가요?",
        a: "네, 컴퓨터활용능력 1급 필기는 상시 CBT 로 진행됩니다. 시험장에서 컴퓨터 화면으로 응시하므로 CBT 환경 적응이 중요해요.",
      },
      {
        q: "2급에서 1급으로 바로 가도 되나요?",
        a: "1급은 데이터베이스 1과목이 추가되고 함수·매크로 난이도가 올라갑니다. 2급 합격 후 1~2주 추가 학습이면 충분히 도전 가능해요.",
      },
      {
        q: "1급 필기 합격하면 2급 필기 면제되나요?",
        a: "네, 1급 필기 합격 후 1년 안에 1급·2급 실기 응시 시 모두 인정됩니다. 별도 2급 필기는 응시할 필요가 없어요.",
      },
    ],
  },
  COMPUTER_LITERACY_2: {
    cert: "COMPUTER_LITERACY_2",
    searchKeyword: "컴활 2급 CBT",
    shortKeyword: "컴활 2급",
    longKeyword: "컴퓨터활용능력 2급 필기",
    title: "컴활 2급 CBT 무료 모의고사 | 컴퓨터활용능력 2급 필기 실전 | 문어CBT",
    description:
      "컴활 2급 CBT 모의고사를 무료로. 컴퓨터활용능력 2급 필기 40문항 40분 실전 타이머, 컴퓨터 일반·스프레드시트 자동 채점과 해설까지 모두 무료로.",
    oneLiner: "컴활 2급 CBT 모의고사 무료 · 40문항 실전 타이머",
    keywords: [
      "컴활 2급 CBT",
      "컴활 2급 모의고사",
      "컴활 2급 무료",
      "컴활 2급 기출",
      "컴퓨터활용능력 2급 CBT",
      "컴퓨터활용능력 2급 모의고사",
      "컴활 필기 CBT",
    ],
    examInfo: {
      questions: "40문항 (객관식 4지선다)",
      duration: "40분",
      passLine: "과목별 40% 이상 + 평균 60점 이상",
      examType: "CBT (컴퓨터 시험)",
      organizer: "대한상공회의소",
    },
    subjects: [
      { name: "1과목. 컴퓨터 일반", description: "운영체제·인터넷·정보보안·멀티미디어 (20문항)" },
      { name: "2과목. 스프레드시트 일반", description: "엑셀 함수·차트·데이터베이스 기능 (20문항)" },
    ],
    studyTip:
      "컴활 2급 CBT 는 1급보다 비중과 난이도가 낮아 기출 복원 2~3회차만 풀어도 합격선 통과가 가능합니다. 함수와 차트, 운영체제 기본 개념만 잡으면 빠르게 합격할 수 있어요.",
    faqs: [
      {
        q: "컴활 2급 CBT 모의고사 무료인가요?",
        a: "네, 문어CBT 의 컴활 2급 CBT 모의고사는 모두 무료입니다. 40문항 실전 타이머와 자동 채점·해설까지 모두 무료로 사용할 수 있어요.",
      },
      {
        q: "컴활 2급 필기는 CBT 인가요?",
        a: "네, 컴퓨터활용능력 2급 필기는 상시 CBT 로 진행됩니다. 컴퓨터 화면에서 답안을 표시하므로 CBT 적응 연습이 효과적입니다.",
      },
      {
        q: "2급은 며칠이면 합격할 수 있나요?",
        a: "기존 컴퓨터 사용 경험이 있다면 3~5일 집중 학습으로도 합격이 가능합니다. 기출 복원 회독이 가장 효율적이에요.",
      },
      {
        q: "취업·이직에 컴활 2급이 도움 되나요?",
        a: "사무직 채용 시 기본 자격으로 인정됩니다. 다만 IT·데이터 직군 지원 시에는 1급이나 SQLD 같은 상위 자격을 추가로 갖추는 게 유리해요.",
      },
    ],
  },
  ADSP: {
    cert: "ADSP",
    searchKeyword: "ADsP CBT",
    shortKeyword: "ADsP",
    longKeyword: "데이터분석 준전문가(ADsP)",
    title: "ADsP CBT 무료 모의고사 | 데이터분석 준전문가 실전 | 문어CBT",
    description:
      "ADsP CBT 모의고사를 무료로. 데이터분석 준전문가 50문항 90분 실전 타이머, 2024 개편 반영 통계 비중까지 자동 채점·해설로 합격선 60점까지 빠르게.",
    oneLiner: "ADsP CBT 모의고사 무료 · 50문항 실전 타이머 · 2024 개편 반영",
    keywords: [
      "ADsP CBT",
      "ADsP 모의고사",
      "ADsP 무료",
      "ADsP 기출",
      "ADsP 2024 개편",
      "데이터분석 준전문가 CBT",
      "데이터분석 준전문가 모의고사",
      "ADsP 통계",
    ],
    examInfo: {
      questions: "50문항 (객관식 4지선다)",
      duration: "90분",
      passLine: "총점 60점 이상 / 과목별 40% 미만 과락",
      examType: "PBT 중심 (CBT 적응 연습으로 활용)",
      organizer: "한국데이터산업진흥원(K-DATA)",
    },
    subjects: [
      { name: "1과목. 데이터 이해", description: "데이터 가치, 빅데이터, 데이터베이스 활용 (10문항)" },
      { name: "2과목. 데이터 분석 기획", description: "분석 마스터 플랜, 분석 거버넌스 (10문항)" },
      { name: "3과목. 데이터 분석", description: "통계 기초, 회귀·분류, 군집화, 텍스트·소셜 분석 (30문항)" },
    ],
    studyTip:
      "ADsP CBT 는 2024년 개편 후 통계 비중이 늘었습니다. 기출 복원에서 통계 계산 문제(평균·분산·회귀계수)를 우선 보고, 3과목 데이터 분석 30문항이 비중이 가장 크니 여기 집중해야 합격선에 안정적으로 진입합니다.",
    faqs: [
      {
        q: "ADsP CBT 모의고사 무료인가요?",
        a: "네, 문어CBT 의 ADsP CBT 모의고사는 모두 무료입니다. 50문항 실전 타이머·자동 채점·과목별 정답률 분석까지 무료로 제공돼요.",
      },
      {
        q: "ADsP 시험은 CBT 인가요 PBT 인가요?",
        a: "ADsP 본 시험은 PBT(종이 시험) 중심이지만, 출제 패턴 적응과 시간 분배 연습에는 CBT 모의고사가 효율적입니다. 본 시험 응시 전 환경에 맞춰 종이로 한 번 더 풀어보세요.",
      },
      {
        q: "2024 개편으로 뭐가 달라졌나요?",
        a: "2024년부터 통계 비중이 강화되어 회귀·분산·평균 계산 문제가 늘었어요. 기존에 출제되던 단순 정의 암기로는 합격이 어렵고, 계산 과정을 익히는 게 중요합니다.",
      },
      {
        q: "비전공자도 ADsP 합격할 수 있나요?",
        a: "가능합니다. 통계 기초만 잘 잡으면 6~8주로 합격권 진입이 충분해요. 3과목 데이터 분석 30문항이 가장 비중 크니 여기를 우선 학습하세요.",
      },
    ],
  },
};
