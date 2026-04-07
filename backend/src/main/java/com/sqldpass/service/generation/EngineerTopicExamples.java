package com.sqldpass.service.generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sqldpass.persistent.question.QuestionType;

/**
 * 정처기 실기 모의고사 생성 시 few-shot 예시로 사용되는 카테고리별 고난도(난이도 5) 시드 1개.
 *
 * 출처: 최상위 폴더의 정보처리기사_실기_샘플문제_27.json (id 3,6,9,12,15,18,21,24,27)
 *
 * AI에게 "이 예시와 동일한 난이도/스타일로 N개 변형 생성"을 시키는 데 사용된다.
 * 코드형(C/Java/Python/SQL/리눅스)은 SHORT_ANSWER, 일부 이론형은 DESCRIPTIVE.
 */
public final class EngineerTopicExamples {

    public record EngineerExample(String topic, QuestionType questionType,
                                  String content, String answer,
                                  List<String> keywords, String explanation, String summary) {
    }

    /** 카테고리(EngineerMockExamCreator 의 카테고리 상수와 일치) → 고난도 예시 1개 */
    public static final Map<String, EngineerExample> EXAMPLES_BY_CATEGORY = new LinkedHashMap<>();

    static {
        EXAMPLES_BY_CATEGORY.put("C언어", new EngineerExample(
                "C - 재귀함수와 구조체",
                QuestionType.SHORT_ANSWER,
                """
                다음 C 코드의 실행 결과를 쓰시오.

                ```c
                #include <stdio.h>
                void func(int n) {
                    if (n <= 0) return;
                    func(n - 2);
                    printf("%d ", n);
                    func(n - 3);
                }
                int main() {
                    func(6);
                    return 0;
                }
                ```""",
                "2 4 1 6 1 3",
                List.of("2 4 1 6 1 3"),
                "func(6) → func(4) → func(2) → 출력 2 → 출력 4 → func(1) → 출력 1 → 출력 6 → func(3) → func(1) → 출력 1 → 출력 3. 최종 출력: 2 4 1 6 1 3",
                "이중 재귀 호출 트레이싱"));

        EXAMPLES_BY_CATEGORY.put("Java", new EngineerExample(
                "Java - 추상클래스와 인터페이스",
                QuestionType.SHORT_ANSWER,
                """
                다음 Java 코드의 실행 결과를 쓰시오.

                ```java
                abstract class Shape {
                    String type = "Shape";
                    abstract int area();
                    String getType() { return type; }
                }
                class Rect extends Shape {
                    String type = "Rect";
                    int w, h;
                    Rect(int w, int h) { this.w = w; this.h = h; }
                    int area() { return w * h; }
                    String getType() { return type; }
                }
                public class Main {
                    public static void main(String[] args) {
                        Shape s = new Rect(3, 4);
                        System.out.println(s.type + " " + s.getType() + " " + s.area());
                    }
                }
                ```""",
                "Shape Rect 12",
                List.of("Shape", "Rect", "12"),
                "필드는 선언 타입(Shape)으로 정적 바인딩되어 \"Shape\". 메서드는 실제 객체(Rect)로 동적 디스패치되어 \"Rect\"와 12 출력.",
                "필드 은닉 vs 메서드 오버라이딩 + 추상클래스"));

        EXAMPLES_BY_CATEGORY.put("Python", new EngineerExample(
                "Python - 클래스와 매직 메서드",
                QuestionType.SHORT_ANSWER,
                """
                다음 Python 코드의 실행 결과를 쓰시오.

                ```python
                def func(x, y=[]):
                    y.append(x)
                    return y

                print(func(1))
                print(func(2))
                print(func(3, []))
                print(func(4))
                ```""",
                "[1]\n[1, 2]\n[3]\n[1, 2, 4]",
                List.of("[1]", "[1, 2]", "[3]", "[1, 2, 4]"),
                "Python의 가변 기본 인자(mutable default argument) 함정. 기본 리스트는 함수 정의 시 한 번만 생성되어 호출 간 공유된다.",
                "가변 기본 인자 함정 (mutable default argument)"));

        EXAMPLES_BY_CATEGORY.put("SQL", new EngineerExample(
                "SQL - 윈도우 함수",
                QuestionType.SHORT_ANSWER,
                """
                다음 [매출] 테이블에서 SQL문을 실행했을 때, 이름이 '이'인 행의 rk 값을 쓰시오.

                | 이름 | 부서 | 금액 |
                |------|------|------|
                | 김 | A | 100 |
                | 이 | A | 200 |
                | 박 | A | 200 |
                | 최 | B | 300 |

                ```sql
                SELECT 이름, RANK() OVER (ORDER BY 금액 DESC) AS rk
                FROM 매출;
                ```""",
                "2",
                List.of("2"),
                "RANK()는 동순위가 있으면 다음 순위를 건너뛴다. 최(300)=1위, 이/박(200)=공동 2위, 김(100)=4위.",
                "RANK 윈도우 함수와 동순위 처리"));

        EXAMPLES_BY_CATEGORY.put("소프트웨어 설계", new EngineerExample(
                "결합도와 응집도",
                QuestionType.DESCRIPTIVE,
                "소프트웨어 모듈의 **결합도(Coupling)**와 **응집도(Cohesion)**의 개념을 설명하고, 좋은 모듈 설계를 위해 각각 어떤 방향으로 설계해야 하는지 서술하시오.",
                "결합도는 모듈 간 상호 의존 정도로 낮을수록 좋고, 응집도는 모듈 내부 요소가 하나의 기능을 위해 얼마나 밀접한지로 높을수록 좋다. 좋은 설계는 낮은 결합도와 높은 응집도를 추구하여 모듈 독립성과 유지보수성을 향상시킨다.",
                List.of("결합도", "응집도", "낮은 결합도", "높은 응집도", "모듈 독립성", "유지보수성"),
                "결합도 유형: 자료(가장 낮음) → 스탬프 → 제어 → 외부 → 공통 → 내용. 응집도 유형: 기능적(가장 높음) → 순차 → 통신 → 절차 → 시간 → 논리 → 우연.",
                "결합도는 낮게, 응집도는 높게 = 좋은 모듈 설계"));

        EXAMPLES_BY_CATEGORY.put("데이터베이스 이론", new EngineerExample(
                "트랜잭션 ACID",
                QuestionType.DESCRIPTIVE,
                "데이터베이스 트랜잭션의 4가지 특성(ACID)을 각각 설명하고, 동시성 제어 기법 중 **2단계 로킹 프로토콜(2PL)**의 원리를 서술하시오.",
                "원자성은 트랜잭션이 모두 실행되거나 전혀 실행되지 않아야 하는 성질, 일관성은 트랜잭션 전후 DB가 일관된 상태를 유지하는 성질, 격리성은 동시 실행 트랜잭션이 서로 영향을 주지 않는 성질, 지속성은 완료된 결과가 영구적으로 반영되는 성질이다. 2PL은 확장 단계에서 Lock 획득, 축소 단계에서 Lock 해제만 허용하여 직렬 가능성을 보장한다.",
                List.of("원자성", "일관성", "격리성", "지속성", "Atomicity", "Consistency", "Isolation", "Durability", "확장 단계", "축소 단계", "직렬 가능성"),
                "2PL은 직렬 가능한 스케줄을 보장하지만 교착 상태(Deadlock)가 발생할 수 있다.",
                "ACID 4특성 + 2PL은 확장/축소 단계로 직렬 가능성 보장"));

        EXAMPLES_BY_CATEGORY.put("네트워크/OS", new EngineerExample(
                "리눅스 명령어와 권한",
                QuestionType.SHORT_ANSWER,
                "리눅스에서 파일의 권한이 현재 `rw-r--r--`일 때, 소유자에게 실행 권한을 추가하고 그룹에게 쓰기 권한을 추가하는 `chmod` 명령어를 8진수 방식으로 작성하시오. (파일명은 test.sh)",
                "chmod 764 test.sh",
                List.of("chmod", "764"),
                "현재 644에서 소유자 +x → 7, 그룹 +w → 6, 기타 그대로 4. 따라서 chmod 764.",
                "chmod 8진수 권한 설정 계산"));

        EXAMPLES_BY_CATEGORY.put("보안", new EngineerExample(
                "보안 공격과 대응",
                QuestionType.DESCRIPTIVE,
                "**SQL 인젝션(SQL Injection)**의 공격 원리를 설명하고, 이를 방어하기 위한 대응 방안을 3가지 이상 서술하시오.",
                "SQL 인젝션은 사용자 입력값을 검증하지 않아 공격자가 입력 필드에 악의적인 SQL 구문을 삽입하여 DB를 비정상적으로 조작하는 공격이다. 대응으로는 1) Prepared Statement(매개변수화 쿼리)로 입력과 SQL을 분리, 2) 입력값 검증과 특수문자 필터링, 3) 웹 방화벽(WAF) 도입, 4) DB 계정에 최소 권한 원칙 적용 등이 있다.",
                List.of("SQL 구문 삽입", "입력값 검증 미흡", "Prepared Statement", "입력값 검증", "WAF", "최소 권한"),
                "OWASP Top 10 대표 취약점. Prepared Statement는 SQL 구조를 미리 컴파일하므로 삽입된 SQL이 구문으로 해석되지 않는다.",
                "입력값에 악성 SQL 삽입 → Prepared Statement 등으로 방어"));

        EXAMPLES_BY_CATEGORY.put("신기술 동향", new EngineerExample(
                "컨테이너와 마이크로서비스",
                QuestionType.DESCRIPTIVE,
                "**컨테이너(Container)** 기술과 **마이크로서비스 아키텍처(MSA)**의 개념을 각각 설명하고, 두 기술이 결합되어 사용되는 이유를 서술하시오.",
                "컨테이너는 애플리케이션과 실행 환경을 패키지로 격리하여 어디서든 동일하게 실행하는 경량 가상화 기술이다. MSA는 애플리케이션을 작고 독립적인 서비스 단위로 분리하여 각 서비스가 독립 배포·확장될 수 있도록 하는 설계 방식이다. 컨테이너는 각 마이크로서비스의 최적 실행 단위를 제공하고, 쿠버네티스 같은 오케스트레이션으로 대규모 관리를 가능하게 하기에 결합되어 사용된다.",
                List.of("컨테이너", "경량 가상화", "격리", "마이크로서비스", "독립 배포", "독립 확장", "Docker", "쿠버네티스"),
                "Docker가 대표적이며, OS 커널 공유로 VM보다 가볍다. 쿠버네티스는 컨테이너 오케스트레이션 플랫폼이다.",
                "컨테이너=경량 격리 실행, MSA=독립 서비스 분리, 결합=최적 배포 단위"));
    }

    public static EngineerExample get(String category) {
        return EXAMPLES_BY_CATEGORY.get(category);
    }

    private EngineerTopicExamples() {
    }
}
