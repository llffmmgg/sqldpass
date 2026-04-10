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
    private static final Map<String, List<String>> ENGINEER_WRITTEN = new LinkedHashMap<>();

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

        // ── 정보처리기사 필기 (2023~2025 기출 기반) ────────────
        ENGINEER_WRITTEN.put("소프트웨어 설계", List.of(
                "UML 다이어그램 종류 (클래스/유스케이스/시퀀스/활동/상태/배치/컴포넌트)",
                "UML 관계 (연관/의존/일반화/실체화/집합/합성)",
                "디자인 패턴 - 생성 (싱글톤/팩토리/빌더/프로토타입)",
                "디자인 패턴 - 구조 (어댑터/브리지/퍼사드/프록시)",
                "디자인 패턴 - 행위 (옵저버/전략/커맨드/상태)",
                "결합도 순서 (내용>공통>외부>제어>스탬프>자료)",
                "응집도 순서 (우연<논리<시간<절차<통신<순차<기능)",
                "요구사항 단계 (도출→분석→명세→확인)",
                "소프트웨어 생명주기 (폭포수/나선형/프로토타입/애자일)",
                "애자일 - 스크럼 (스프린트/백로그/번다운)",
                "애자일 - XP (TDD/짝프로그래밍/리팩토링/CI)",
                "UI 유형 (CLI/GUI/NUI/OUI)",
                "UI 설계원칙 (직관성/유효성/학습성/유연성)",
                "아키텍처 패턴 (MVC/파이프필터/레이어드/클라이언트서버/브로커)",
                "품질 특성 (ISO 9126 / ISO 25010)",
                "와이어프레임 / 목업 / 프로토타입 / 스토리보드",
                "HCI / UX / 감성공학",
                "코드 설계 (순차/블록/10진/그룹/연상/표의숫자)",
                "인터페이스 설계 (EAI/ESB 연계방식)"));
        ENGINEER_WRITTEN.put("소프트웨어 개발", List.of(
                "스택 / 큐 / 데크",
                "연결리스트 (단일/이중/원형)",
                "트리 (이진/완전이진/힙/BST)",
                "트리 순회 (전위/중위/후위/레벨)",
                "그래프 (인접행렬/인접리스트/DFS/BFS)",
                "해싱 (해시함수/충돌해결: 체이닝/개방주소법)",
                "정렬 알고리즘 (버블/선택/삽입/퀵/합병/힙/기수) — 시간복잡도",
                "검색 알고리즘 (순차/이진/해시)",
                "테스트 유형 (단위/통합/시스템/인수)",
                "화이트박스 (기본경로/조건/분기)",
                "블랙박스 (동치분할/경계값/원인결과/상태전이)",
                "통합테스트 (상향식: 드라이버 | 하향식: 스텁 | 빅뱅/샌드위치)",
                "커버리지 (구문/결정/조건/MC-DC)",
                "형상관리 SCM (Git/SVN/CVS)",
                "빌드 자동화 (Jenkins/Gradle/Maven/Ant)",
                "미들웨어 (WAS/MOM/RPC/ORB/TP모니터)",
                "인터페이스 검증 (xUnit/STAF/FitNesse/Watir)",
                "릴리즈 노트 / DRM",
                "테스트 하네스 / 테스트 오라클",
                "JSON / XML / AJAX / REST 구분"));
        ENGINEER_WRITTEN.put("데이터베이스 구축", List.of(
                "정규화 (1NF→2NF→3NF→BCNF) — 이상현상 연계",
                "키 종류 (슈퍼키/후보키/기본키/대체키/외래키)",
                "무결성 제약 (개체/참조/도메인)",
                "SQL DDL (CREATE/ALTER/DROP/TRUNCATE)",
                "SQL DML (SELECT/INSERT/UPDATE/DELETE)",
                "JOIN (INNER/LEFT/RIGHT/FULL/CROSS/SELF)",
                "관계대수 (σ Select/π Project/⋈ Join/÷ Division)",
                "트랜잭션 ACID",
                "DCL (GRANT/REVOKE) / TCL (COMMIT/ROLLBACK/SAVEPOINT)",
                "집계함수 (COUNT/SUM/AVG/MAX/MIN) + GROUP BY/HAVING",
                "서브쿼리 (스칼라/인라인뷰/WHERE절)",
                "윈도우함수 (RANK/DENSE_RANK/ROW_NUMBER)",
                "관계해석 (튜플/도메인)",
                "이상현상 (삽입/삭제/갱신)",
                "관계 데이터 모델 용어 (릴레이션/튜플/속성/도메인/카디널리티/차수)",
                "반정규화 (테이블 분할/병합/중복)",
                "인덱스 (B-Tree/비트맵/클러스터드)",
                "뷰 / 시퀀스 / 파티셔닝",
                "ETL / 데이터 마이그레이션",
                "분산 데이터베이스 / 데이터 품질(DQM)"));
        ENGINEER_WRITTEN.put("프로그래밍 언어 활용", List.of(
                "C언어 - 포인터 산술 / 포인터와 배열",
                "C언어 - 구조체 / 공용체",
                "C언어 - 비트연산 (&, |, ^, ~, <<, >>)",
                "C언어 - 재귀함수 추적",
                "C언어 - 문자열 함수 (strlen/strcpy/strcmp/strcat)",
                "Java - 상속 / 오버라이딩 출력 추적",
                "Java - 추상클래스 / 인터페이스 구분",
                "Java - ArrayList / HashMap 조작",
                "Java - 예외처리 흐름 (try-catch-finally)",
                "Python - 리스트 슬라이싱 / 컴프리헨션",
                "Python - 딕셔너리 조작",
                "Python - 함수 / 람다",
                "Python - 클래스 상속",
                "코드 출력 추적 - 반복문 + 조건문 복합",
                "코드 출력 추적 - 함수 호출 스택",
                "프로세스 상태 전이 (생성→준비→실행→대기→종료)",
                "스케줄링 (FCFS/SJF/RR/우선순위/SRT/HRN)",
                "교착상태 (4조건/예방/회피/탐지/복구)",
                "페이징 / 세그먼테이션",
                "페이지 교체 (FIFO/LRU/LFU/OPT)",
                "OSI 7계층 (물데네전세표응) — 각 계층 프로토콜",
                "TCP vs UDP",
                "서브넷 마스크 / CIDR 계산",
                "프로토콜 (HTTP/FTP/SMTP/SNMP/ARP/ICMP)",
                "리눅스 명령어 (ls/cd/chmod/chown/grep/find/cat/ps/kill)"));
        ENGINEER_WRITTEN.put("정보시스템 구축 관리", List.of(
                "보안 3요소 (기밀성/무결성/가용성 = CIA)",
                "대칭키 (AES/DES/3DES/SEED/ARIA/IDEA)",
                "비대칭키 (RSA/ECC/DSA/디피헬먼/ElGamal)",
                "해시 (SHA-256/MD5/HAS-160)",
                "접근통제 (MAC/DAC/RBAC)",
                "입력데이터 검증 (SQL Injection/XSS/CSRF)",
                "DoS/DDoS (SYN Flood/Smurf/Land/Ping of Death/Teardrop/Slowloris)",
                "네트워크 보안 (방화벽/IDS/IPS/WAF/DMZ)",
                "보안 솔루션 (VPN/NAC/ESM/SIEM/DLP/UTM)",
                "개발방법론 (구조적/객체지향/CBD/SOA)",
                "보안 개발방법론 (MS-SDL/CLASP/Seven Touchpoints)",
                "프로젝트 관리 (WBS/간트/PERT/CPM — 임계경로)",
                "비용산정 (LOC/COCOMO/FP/PUTNAM)",
                "네트워크 구축 (VLAN/VPN/NAT/SDN/NFV)",
                "클라우드 (IaaS/PaaS/SaaS/온프레미스)",
                "스토리지 (DAS/NAS/SAN)",
                "RAID (0/1/5/6/10)",
                "AI/머신러닝/딥러닝 (CNN/RNN/GAN/트랜스포머)",
                "블록체인 / 핀테크 / 디지털트윈 / 메타버스",
                "컨테이너(도커) / 쿠버네티스 / 클라우드 네이티브",
                "DevOps / CI·CD / MSA / 서비스 메시",
                "엣지컴퓨팅 / 포그컴퓨팅 / IoT",
                "빅데이터 (하둡/맵리듀스/스파크/데이터레이크)",
                "제로트러스트 / 양자암호 / SBOM"));
    }

    private SubTopicCatalog() {
    }

    /** 자격증 + 카테고리(과목명) 에 대응하는 세부 토픽 풀. 매칭 없으면 빈 리스트. */
    public static List<String> forSubject(String subjectName, ExamType examType) {
        return switch (examType) {
            case SQLD -> SQLD.getOrDefault(subjectName, List.of());
            case ENGINEER_PRACTICAL -> ENGINEER.getOrDefault(subjectName, List.of());
            case COMPUTER_LITERACY_1 -> COMPUTER_LITERACY.getOrDefault(subjectName, List.of());
            case ENGINEER_WRITTEN -> ENGINEER_WRITTEN.getOrDefault(subjectName, List.of());
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
