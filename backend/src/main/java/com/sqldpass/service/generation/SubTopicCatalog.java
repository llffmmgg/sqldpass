package com.sqldpass.service.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.sqldpass.persistent.mockexam.ExamType;

/**
 * 자격증 카테고리(과목명) 별 세부 토픽 catalog — 단일 source of truth.
 *
 * 문제 생성 호출 시 N개 세부 토픽을 균등 분포로 추출하여 PromptBuilder 의 강제 토픽
 * instruction 으로 주입한다. 시드 example 의 토픽이 catalog 와 다르더라도 catalog 가 우선.
 *
 * 새 자격증/과목 추가 시 이 파일 한 곳만 수정하면 된다.
 *
 * Subject 트리(DB)의 leaf 과목명과 정확히 일치해야 하므로,
 * SubjectEntity name 변경 시 catalog 키도 함께 수정 필수.
 */
public final class SubTopicCatalog {

    private static final Map<String, List<String>> SQLD = new LinkedHashMap<>();
    private static final Map<String, List<String>> ENGINEER = new LinkedHashMap<>();
    private static final Map<String, List<String>> COMPUTER_LITERACY = new LinkedHashMap<>();
    private static final Map<String, List<String>> COMPUTER_LITERACY_2 = new LinkedHashMap<>();
    private static final Map<String, List<String>> ENGINEER_WRITTEN = new LinkedHashMap<>();
    private static final Map<String, List<String>> ADSP = new LinkedHashMap<>();

    static {
        // ── SQLD ────────────────────────────────────────────
        SQLD.put("데이터 모델링의 이해", List.of(
                "엔터티 정의",
                "엔터티 종류 (강 vs 약, 유형 vs 사건)",
                "속성 분류 (단순/복합, 단일값/다중값, 유도)",
                "관계 (1:1, 1:N, M:N)",
                "관계 차수와 카디널리티",
                "기본키",
                "후보키",
                "대체키",
                "슈퍼키",
                "외래키"));
        SQLD.put("데이터 모델과 SQL", List.of(
                "정규화 1NF",
                "정규화 2NF",
                "정규화 3NF",
                "BCNF",
                "반정규화 - 테이블 통합",
                "반정규화 - 컬럼 중복",
                "반정규화 - 이력 테이블",
                "성능 데이터 모델링 - 분산"));
        SQLD.put("SQL 기본", List.of(
                "SELECT 기본 구조",
                "WHERE 비교 연산자",
                "WHERE NULL 처리 (IS NULL / IS NOT NULL)",
                "WHERE LIKE / IN / BETWEEN",
                "GROUP BY",
                "HAVING (vs WHERE)",
                "ORDER BY (정렬 + NULL 처리)",
                "단일행 함수 - 문자",
                "단일행 함수 - 숫자",
                "단일행 함수 - 날짜",
                "NVL / COALESCE / NULLIF",
                "DDL (CREATE/ALTER/DROP/TRUNCATE)",
                "DML (INSERT/UPDATE/DELETE/MERGE)",
                "TCL (COMMIT/ROLLBACK/SAVEPOINT)",
                "제약조건 (PK/FK/UK/CHECK/NOT NULL)"));
        SQLD.put("SQL 활용", List.of(
                "INNER JOIN",
                "LEFT OUTER JOIN",
                "RIGHT OUTER JOIN",
                "FULL OUTER JOIN",
                "SELF JOIN",
                "CROSS JOIN / Cartesian product",
                "단일행 서브쿼리",
                "다중행 서브쿼리 - IN",
                "다중행 서브쿼리 - ANY / SOME",
                "다중행 서브쿼리 - ALL",
                "상관 서브쿼리",
                "EXISTS / NOT EXISTS",
                "스칼라 서브쿼리",
                "집합연산자 - UNION vs UNION ALL",
                "집합연산자 - INTERSECT / MINUS(EXCEPT)",
                "ROLLUP",
                "CUBE",
                "GROUPING SETS",
                "ROW_NUMBER",
                "RANK (동순위 처리)",
                "DENSE_RANK",
                "LAG / LEAD",
                "윈도우 PARTITION BY",
                "윈도우 프레임 (ROWS/RANGE)",
                "계층형 질의 (CONNECT BY / START WITH)",
                "PIVOT / UNPIVOT",
                "정규표현식 함수",
                "COUNT (* vs 컬럼)",
                "SUM / AVG (NULL 무시)",
                "DISTINCT",
                "JOIN 결과 행 수 예측",
                "GROUP BY + 집계 결과 예측",
                "서브쿼리 결과 예측"));
        SQLD.put("관리 구문", List.of(
                "DCL - GRANT / REVOKE",
                "ROLE",
                "옵티마이저 종류 (RBO / CBO)",
                "실행 계획",
                "B-Tree 인덱스",
                "Bitmap 인덱스",
                "인덱스가 안 타는 경우"));

        // ── 정보처리기사 실기 ─────────────────────────────────
        // 시드에 이미 든 핵심 영역(C 포인터/배열/비트/재귀, Java 다형성/Stream, Python 가변인자/슬라이싱
        // 등)은 catalog 에서도 포함하되, 시드에 부족한 영역(소프트웨어 설계/DB 이론/OS/네트워크/보안/신기술)
        // 위주로 풍부하게 작성한다.
        ENGINEER.put("C언어", List.of(
                "포인터 산술",
                "포인터와 배열",
                "이중 포인터",
                "함수 포인터",
                "문자열 처리 (strlen, strcpy, strcmp)",
                "구조체 / typedef",
                "비트 연산",
                "재귀 함수",
                "동적 할당 (malloc/free)"));
        ENGINEER.put("Java", List.of(
                "클래스와 객체 기본",
                "상속 vs 인터페이스",
                "다형성과 동적 디스패치",
                "추상 클래스",
                "ArrayList / HashMap / HashSet",
                "Iterator / 향상된 for문",
                "String / StringBuilder",
                "예외 처리 (try-catch-finally)",
                "Comparator / 람다",
                "Stream API"));
        ENGINEER.put("Python", List.of(
                "기본 문법 (변수/타입)",
                "리스트 / 튜플",
                "딕셔너리 / 셋",
                "for / while 반복문",
                "함수 정의와 인자",
                "가변 기본 인자 함정",
                "리스트 컴프리헨션",
                "슬라이싱",
                "클로저 / 데코레이터"));
        ENGINEER.put("SQL", List.of(
                "INNER JOIN 결과",
                "LEFT OUTER JOIN + NULL",
                "GROUP BY + HAVING",
                "단일행 / 다중행 서브쿼리",
                "상관 서브쿼리",
                "RANK / DENSE_RANK / ROW_NUMBER",
                "LAG / LEAD",
                "윈도우 PARTITION BY",
                "정규화 1NF / 2NF / 3NF"));
        ENGINEER.put("소프트웨어 설계", List.of(
                "UML 클래스 다이어그램",
                "UML 유스케이스 다이어그램",
                "UML 시퀀스 다이어그램",
                "UML 활동 다이어그램",
                "디자인 패턴 - 싱글톤",
                "디자인 패턴 - 팩토리",
                "디자인 패턴 - 옵저버",
                "디자인 패턴 - 전략",
                "디자인 패턴 - 어댑터",
                "디자인 패턴 - 데코레이터",
                "요구사항 분석 - 기능 vs 비기능",
                "요구사항 도출 기법",
                "응집도 vs 결합도",
                "객체지향 5원칙 (SOLID)"));
        ENGINEER.put("데이터베이스 이론", List.of(
                "트랜잭션 ACID",
                "트랜잭션 격리수준",
                "락 (공유락 vs 배타락)",
                "교착 상태와 회피",
                "B-Tree 인덱스 원리",
                "클러스터드 vs 비클러스터드 인덱스",
                "무결성 제약 (개체/참조/도메인)",
                "이상 현상 (삽입/삭제/수정)",
                "정규화 vs 반정규화"));
        ENGINEER.put("네트워크/OS", List.of(
                "프로세스 vs 스레드",
                "프로세스 상태 전이",
                "CPU 스케줄링 - FCFS / SJF",
                "CPU 스케줄링 - Round Robin",
                "CPU 스케줄링 - 우선순위",
                "교착상태 4가지 조건",
                "교착상태 회피 (은행원 알고리즘)",
                "페이징 vs 세그먼테이션",
                "가상 메모리 / 페이지 폴트",
                "OSI 7계층 식별",
                "TCP 3-way handshake",
                "TCP vs UDP 특성",
                "HTTP 메서드 / 상태 코드",
                "HTTP vs HTTPS / TLS",
                "IP 주소 (IPv4/IPv6)",
                "서브넷 마스크 / CIDR",
                "DNS / DHCP",
                "라우팅 프로토콜"));
        ENGINEER.put("보안", List.of(
                "대칭키 암호화 - DES / AES",
                "비대칭키 암호화 - RSA",
                "대칭 vs 비대칭 비교",
                "해시 함수 - SHA / MD5",
                "전자 서명",
                "인증 (Authentication) vs 인가 (Authorization)",
                "OAuth / JWT",
                "공격 - SQL Injection",
                "공격 - XSS",
                "공격 - CSRF",
                "공격 - 버퍼 오버플로우",
                "보안 3요소 (CIA)"));
        ENGINEER.put("자료구조/알고리즘", List.of(
                "스택과 큐",
                "연결 리스트",
                "트리 (이진 트리 / BST)",
                "그래프 (DFS / BFS)",
                "해싱 (해시 테이블 / 충돌 해결)",
                "정렬 알고리즘 (버블/선택/삽입/퀵/병합)",
                "탐색 알고리즘 (순차/이진 탐색)",
                "시간 복잡도 (Big-O)"));
        ENGINEER.put("신기술 동향", List.of(
                "AI / 머신러닝 기본 개념",
                "지도학습 vs 비지도학습",
                "딥러닝 / 신경망 / CNN",
                "클라우드 (IaaS / PaaS / SaaS)",
                "도커 / 컨테이너",
                "쿠버네티스",
                "마이크로서비스",
                "블록체인 / 분산원장",
                "IoT / 엣지 컴퓨팅",
                "빅데이터 5V"));

        // ── 컴퓨터활용능력 1급 필기 ──────────────────────────
        COMPUTER_LITERACY.put("컴퓨터 일반", List.of(
                "운영체제 기본 (역할/종류)",
                "Windows 작업 관리자 / 제어판",
                "CPU 구성 (제어/연산/레지스터)",
                "메모리 - RAM / ROM",
                "메모리 - 캐시 메모리 / 가상 메모리",
                "저장장치 - HDD vs SSD",
                "RAID",
                "입출력 장치 / 인터페이스 (USB/HDMI)",
                "네트워크 기본 (LAN / WAN / MAN)",
                "TCP/IP 4계층",
                "인터넷 서비스 (WWW / FTP / 이메일)",
                "IP 주소 / 도메인 / DNS",
                "정보 보안 기초 (방화벽/백신)",
                "암호화 / 인증",
                "멀티미디어 - 이미지 포맷 (JPG/PNG/GIF/BMP)",
                "멀티미디어 - 사운드 포맷 (WAV/MP3)",
                "멀티미디어 - 동영상 포맷 (AVI/MP4/MOV)",
                "압축 (ZIP/RAR/7Z)"));
        COMPUTER_LITERACY.put("스프레드시트 일반", List.of(
                "VLOOKUP / HLOOKUP",
                "INDEX / MATCH",
                "IF / IFS / NESTED IF",
                "COUNTIF / COUNTIFS",
                "SUMIF / SUMIFS",
                "SUM / AVERAGE / COUNT / MAX / MIN",
                "ROUND / INT / TRUNC / MOD",
                "TEXT 함수 - LEFT/RIGHT/MID/LEN",
                "DATE 함수 - TODAY/NOW/YEAR/MONTH",
                "절대참조 vs 상대참조 vs 혼합참조",
                "이름 정의 (named range)",
                "정렬 (단일/다중 키)",
                "필터 (자동 필터)",
                "필터 (고급 필터)",
                "조건부 서식",
                "데이터 유효성 검사",
                "피벗 테이블 - 행/열/값/필터",
                "피벗 테이블 - 그룹화 / 슬라이서",
                "피벗 차트",
                "차트 종류 - 막대/꺾은선/원/분산형/방사형",
                "차트 요소 (제목/범례/레이블/축)",
                "매크로 / VBA 기본",
                "매크로 보안 / 절대·상대 매크로"));
        COMPUTER_LITERACY.put("데이터베이스 일반", List.of(
                "관계형 DB 모델",
                "기본키 / 외래키 / 후보키",
                "정규화 개념",
                "SQL SELECT",
                "SQL WHERE / ORDER BY / GROUP BY",
                "SQL INSERT / UPDATE / DELETE",
                "JOIN 기본",
                "Access 폼 / 보고서 / 쿼리",
                "Access 매크로"));

        // ── 컴퓨터활용능력 2급 필기 ──────────────────────────
        COMPUTER_LITERACY_2.put("컴퓨터 일반", List.of(
                "컴퓨터 시스템 개요 (세대/자료표현/진법)",
                "하드웨어 (CPU/메모리/입출력장치)",
                "소프트웨어 (운영체제/언어 번역기/프로세스)",
                "Windows 활용 (제어판/파일관리/단축키/레지스트리)",
                "네트워크와 인터넷 (프로토콜/IP/OSI/웹)",
                "정보 보안 (바이러스/방화벽/암호화)",
                "멀티미디어 (파일 형식/래스터·벡터/압축)",
                "ICT 신기술 (IoT/클라우드/블록체인/AI)"));
        COMPUTER_LITERACY_2.put("스프레드시트 일반", List.of(
                "데이터 입력과 편집 (자동 채우기/유효성/메모·노트)",
                "셀 서식 (표시 형식 코드/조건부 서식)",
                "수식과 함수 (SUM/IF/IFS/COUNTIF/ROUND/문자열)",
                "수식과 함수 - 찾기/참조 (VLOOKUP/셀참조/SWITCH)",
                "차트 (종류/속성/보조 축/셀 연동)",
                "데이터 관리 (정렬/고급 필터/부분합)",
                "인쇄와 페이지 설정 (인쇄 제목/머리글·바닥글/배율)",
                "매크로와 VBA (기본 개념/보안/프로시저)",
                "피벗 테이블 (영역/값 표시 형식)"));

        // ── 정보처리기사 필기 (2023~2025 기출 기반, 시드 토픽명과 정확히 일치) ──
        ENGINEER_WRITTEN.put("소프트웨어 설계", List.of(
                "UML 다이어그램 종류", "UML 관계", "디자인 패턴",
                "결합도와 응집도", "요구사항 분석",
                "소프트웨어 생명주기", "애자일 (스크럼/XP)",
                "UI 설계", "아키텍처 패턴", "품질 특성 (ISO 9126 / 25010)"));
        ENGINEER_WRITTEN.put("소프트웨어 개발", List.of(
                "자료구조 - 스택/큐", "자료구조 - 트리", "자료구조 - 그래프/해싱",
                "정렬 알고리즘", "테스트 유형", "화이트박스/블랙박스 테스트",
                "통합 테스트", "형상관리/빌드 자동화", "미들웨어", "인터페이스 구현"));
        ENGINEER_WRITTEN.put("데이터베이스 구축", List.of(
                "정규화", "키 종류", "무결성 제약", "SQL DDL",
                "SQL DML / JOIN", "집계함수 / GROUP BY / HAVING",
                "서브쿼리", "윈도우 함수", "관계대수",
                "트랜잭션 ACID / DCL / TCL", "인덱스 / 반정규화"));
        ENGINEER_WRITTEN.put("프로그래밍 언어 활용", List.of(
                "C언어 - 포인터/배열", "C언어 - 비트연산/재귀", "C언어 - 문자열/구조체",
                "Java - 클래스/상속", "Java - 컬렉션/예외처리",
                "Python - 리스트/딕셔너리", "Python - 함수/컴프리헨션",
                "OS - 프로세스/스케줄링", "OS - 교착상태/페이지 교체",
                "네트워크 - OSI/프로토콜", "리눅스 명령어"));
        ENGINEER_WRITTEN.put("정보시스템 구축 관리", List.of(
                "보안 3요소 (CIA)", "암호화 알고리즘", "접근 통제",
                "웹 보안 공격", "DoS/DDoS 공격", "네트워크 보안 솔루션",
                "개발 방법론 / 비용산정", "클라우드 / 스토리지", "신기술 동향",
                "네트워크 기술", "Secure SDLC / 보안 개발 방법론"));

        // ── 데이터분석 준전문가 ADsP (2024 개편 반영) ──────────
        ADSP.put("데이터 이해", List.of(
                "DIKW 피라미드 (데이터/정보/지식/지혜)",
                "데이터베이스 정의·특성 (통합/저장/공유/운영)",
                "데이터베이스 발전 단계",
                "OLTP / OLAP 차이",
                "CRM / SCM / ETL / 데이터 마이닝",
                "빅데이터 3V~5V",
                "빅데이터 출현 배경·기능",
                "빅데이터 가치와 위기요인·통제방안",
                "데이터 사이언스 학제적 성격",
                "데이터 사이언티스트 Hard/Soft Skill",
                "분석 주제 유형 (최적화/솔루션/통찰/발견)"));
        ADSP.put("데이터 분석 기획", List.of(
                "분석 방법론 - KDD / CRISP-DM / SEMMA 비교",
                "분석과제 발굴 - 하향식(Top-Down)",
                "분석과제 발굴 - 상향식(Bottom-Up)",
                "분석 프로젝트 관리 (WBS/위험관리/품질)",
                "위험 대응 전략 (회피/전가/완화/수용)",
                "분석 마스터플랜과 우선순위",
                "ISP와의 연계",
                "분석 거버넌스 체계 (조직/프로세스/시스템/데이터)",
                "분석 성숙도 모델 (도입/활용/확산/최적화)",
                "MDM / 메타데이터 / 데이터 품질관리"));
        ADSP.put("데이터 분석", List.of(
                "R 데이터 타입과 기본 함수",
                "R NA/결측 처리 (na.rm)",
                "데이터마트와 EDA",
                "결측값/이상값 처리",
                "기술통계 vs 추론통계",
                "가설검정 (1종/2종 오류)",
                "신뢰구간",
                "단순/다중 회귀분석",
                "로지스틱 회귀",
                "릿지(L2) / 라쏘(L1) 회귀",
                "의사결정나무 불순도 (지니/엔트로피)",
                "KNN / SVM / 나이브베이즈",
                "배깅 / 부스팅 / 랜덤포레스트",
                "인공신경망·딥러닝 과적합 방지",
                "K-means 군집분석",
                "계층적 군집 연결법",
                "거리 측도 (유클리디안/맨해튼/체비셰프)",
                "연관분석 - 지지도·신뢰도·향상도",
                "시계열 정상성과 ARIMA",
                "지수 평활법",
                "PCA 차원축소와 주성분 선택",
                "MDS 다차원척도법",
                "혼동행렬 - 정밀도/재현율/정확도",
                "ROC 곡선과 AUC"));
    }

    private SubTopicCatalog() {
    }

    /** 자격증 + 카테고리(과목명) 에 대응하는 세부 토픽 풀. 매칭 없으면 빈 리스트. */
    public static List<String> forSubject(String subjectName, ExamType examType) {
        return switch (examType) {
            case SQLD -> SQLD.getOrDefault(subjectName, List.of());
            case ENGINEER_PRACTICAL -> ENGINEER.getOrDefault(subjectName, List.of());
            case COMPUTER_LITERACY_1 -> COMPUTER_LITERACY.getOrDefault(subjectName, List.of());
            case COMPUTER_LITERACY_2 -> COMPUTER_LITERACY_2.getOrDefault(subjectName, List.of());
            case ENGINEER_WRITTEN -> ENGINEER_WRITTEN.getOrDefault(subjectName, List.of());
            case ADSP -> ADSP.getOrDefault(subjectName, List.of());
        };
    }

    /**
     * N개의 세부 토픽을 균등 분포로 추출 (랜덤 셔플 후 round-robin).
     * pool 이 비어 있으면 빈 리스트 반환 — 호출부에서 fallback 처리 가능.
     */
    public static List<String> pickN(String subjectName, ExamType examType, int n, Random rng) {
        List<String> pool = forSubject(subjectName, examType);
        if (pool.isEmpty() || n <= 0) return List.of();
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, rng);
        List<String> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(shuffled.get(i % shuffled.size()));
        }
        return result;
    }
}
