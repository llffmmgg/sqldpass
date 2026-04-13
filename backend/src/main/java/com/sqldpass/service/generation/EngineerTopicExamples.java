package com.sqldpass.service.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sqldpass.persistent.question.QuestionType;

/**
 * 정처기 실기 모의고사 생성 시 few-shot 예시로 사용되는 카테고리별 시드 풀.
 *
 * 카테고리당 5개의 서로 다른 패턴 시드를 보유하며, 각 시드는 난이도(1/3/5) 메타를 가진다.
 * 모의고사 생성 시 카테고리별로 needed 만큼 시드를 풀에서 무작위 추출하여
 * 각 시드를 "퀄리티 기준 + 회피 대상"으로 동시에 사용한다.
 *
 * AI는 시드의 변수명/클래스명/함수명을 절대 복제하지 않고, 동일한 깊이의 다른 개념으로
 * 변형 1개씩을 생성한다.
 */
public final class EngineerTopicExamples {

    public record EngineerExample(String topic, QuestionType questionType,
                                  String content, String answer,
                                  List<String> keywords, String explanation, String summary,
                                  int difficulty) {
    }

    /** 카테고리 → 시드 풀 (5개씩) */
    public static final Map<String, List<EngineerExample>> EXAMPLES_BY_CATEGORY = new LinkedHashMap<>();

    static {
        // ============================================================
        // C언어 (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("C언어", List.of(
                new EngineerExample(
                        "C - 이중 재귀 트레이싱",
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
                        "func(6) → func(4) → func(2) → 출력 2 → 출력 4 → func(1) → 출력 1 → 출력 6 → func(3) → func(1) → 출력 1 → 출력 3.",
                        "이중 재귀 호출 트레이싱",
                        5),
                new EngineerExample(
                        "C - 단순 반복문 합계",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 C 코드의 실행 결과를 쓰시오.

                        ```c
                        #include <stdio.h>
                        int main() {
                            int sum = 0;
                            for (int i = 1; i <= 5; i++) {
                                if (i % 2 == 0) sum += i;
                            }
                            printf("%d", sum);
                            return 0;
                        }
                        ```""",
                        "6",
                        List.of("6"),
                        "1~5 중 짝수는 2, 4. 합계는 2 + 4 = 6.",
                        "for문 + 조건 + 누적합 기본",
                        1),
                new EngineerExample(
                        "C - 포인터 산술과 배열",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 C 코드의 실행 결과를 쓰시오.

                        ```c
                        #include <stdio.h>
                        int main() {
                            int data[5] = {10, 20, 30, 40, 50};
                            int *ptr = data + 2;
                            printf("%d %d", *(ptr - 1), *(ptr + 2));
                            return 0;
                        }
                        ```""",
                        "20 50",
                        List.of("20 50"),
                        "ptr은 data[2](=30)을 가리킴. ptr-1은 data[1](=20), ptr+2는 data[4](=50).",
                        "포인터 산술 인덱싱",
                        3),
                new EngineerExample(
                        "C - 비트 연산자",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 C 코드의 실행 결과를 쓰시오.

                        ```c
                        #include <stdio.h>
                        int main() {
                            int a = 12;
                            int b = 10;
                            printf("%d %d %d", a & b, a | b, a ^ b);
                            return 0;
                        }
                        ```""",
                        "8 14 6",
                        List.of("8 14 6"),
                        "12=1100, 10=1010. AND=1000(8), OR=1110(14), XOR=0110(6).",
                        "비트 AND/OR/XOR 계산",
                        3),
                new EngineerExample(
                        "C - 구조체 배열과 typedef",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 C 코드의 실행 결과를 쓰시오.

                        ```c
                        #include <stdio.h>
                        typedef struct {
                            int id;
                            int score;
                        } Item;
                        int main() {
                            Item arr[3] = {{1, 80}, {2, 90}, {3, 70}};
                            Item *p = arr;
                            int total = 0;
                            for (int i = 0; i < 3; i++) {
                                total += (p + i)->score;
                            }
                            printf("%d", total);
                            return 0;
                        }
                        ```""",
                        "240",
                        List.of("240"),
                        "구조체 배열을 포인터로 순회. 80 + 90 + 70 = 240.",
                        "구조체 + typedef + 포인터 순회",
                        5)
        ));

        // ============================================================
        // Java (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("Java", List.of(
                new EngineerExample(
                        "Java - 추상클래스 동적 디스패치",
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
                        "필드는 선언 타입(Shape)으로 정적 바인딩되어 \"Shape\". 메서드는 실제 객체(Rect)로 동적 디스패치.",
                        "필드 은닉 vs 메서드 오버라이딩",
                        5),
                new EngineerExample(
                        "Java - 문자열 메서드 기본",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Java 코드의 실행 결과를 쓰시오.

                        ```java
                        public class Main {
                            public static void main(String[] args) {
                                String text = "Hello World";
                                System.out.println(text.substring(6).toLowerCase());
                            }
                        }
                        ```""",
                        "world",
                        List.of("world"),
                        "substring(6)은 인덱스 6부터 끝까지 → \"World\". toLowerCase()로 \"world\".",
                        "String substring/toLowerCase 기본",
                        1),
                new EngineerExample(
                        "Java - try-catch-finally 실행 순서",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Java 코드의 실행 결과를 쓰시오.

                        ```java
                        public class Main {
                            static int run() {
                                try {
                                    return 1;
                                } catch (Exception e) {
                                    return 2;
                                } finally {
                                    System.out.print("F");
                                }
                            }
                            public static void main(String[] args) {
                                System.out.print(run());
                            }
                        }
                        ```""",
                        "F1",
                        List.of("F1"),
                        "try의 return 1이 결정되지만 finally가 먼저 실행되어 \"F\" 출력 → 그 후 1 반환 → \"F1\".",
                        "try-return-finally 실행 순서",
                        3),
                new EngineerExample(
                        "Java - Comparator 람다 정렬",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Java 코드의 실행 결과를 쓰시오.

                        ```java
                        import java.util.*;
                        public class Main {
                            public static void main(String[] args) {
                                List<String> list = new ArrayList<>(Arrays.asList("apple", "kiwi", "banana"));
                                list.sort((a, b) -> a.length() - b.length());
                                System.out.println(list.get(0) + " " + list.get(2));
                            }
                        }
                        ```""",
                        "kiwi banana",
                        List.of("kiwi banana"),
                        "길이 오름차순 정렬: kiwi(4), apple(5), banana(6). 첫번째 \"kiwi\", 세번째 \"banana\".",
                        "Comparator 람다 + 길이 정렬",
                        3),
                new EngineerExample(
                        "Java - Stream reduce",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Java 코드의 실행 결과를 쓰시오.

                        ```java
                        import java.util.*;
                        import java.util.stream.*;
                        public class Main {
                            public static void main(String[] args) {
                                int result = Stream.of(1, 2, 3, 4, 5)
                                        .filter(x -> x % 2 == 1)
                                        .reduce(0, (acc, x) -> acc + x * x);
                                System.out.println(result);
                            }
                        }
                        ```""",
                        "35",
                        List.of("35"),
                        "홀수만 필터링: 1, 3, 5. 각각 제곱한 합: 1 + 9 + 25 = 35.",
                        "Stream filter + reduce 누적",
                        5)
        ));

        // ============================================================
        // Python (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("Python", List.of(
                new EngineerExample(
                        "Python - 가변 기본 인자 함정",
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
                        "가변 기본 인자는 함수 정의 시 한 번만 생성되어 호출 간 공유된다.",
                        "가변 기본 인자 함정",
                        5),
                new EngineerExample(
                        "Python - 리스트 슬라이싱",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Python 코드의 실행 결과를 쓰시오.

                        ```python
                        nums = [10, 20, 30, 40, 50]
                        print(nums[1:4])
                        ```""",
                        "[20, 30, 40]",
                        List.of("[20, 30, 40]"),
                        "슬라이싱 [1:4]는 인덱스 1부터 3까지(끝 인덱스 4 제외).",
                        "리스트 슬라이싱 기본",
                        1),
                new EngineerExample(
                        "Python - 딕셔너리 순회와 정렬",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Python 코드의 실행 결과를 쓰시오.

                        ```python
                        scores = {"a": 80, "b": 95, "c": 70}
                        result = sorted(scores.items(), key=lambda x: x[1], reverse=True)
                        print(result[0][0], result[2][0])
                        ```""",
                        "b c",
                        List.of("b c"),
                        "값 내림차순 정렬: [(b,95), (a,80), (c,70)]. result[0][0]=\"b\", result[2][0]=\"c\".",
                        "딕셔너리 items() + lambda 정렬",
                        3),
                new EngineerExample(
                        "Python - 리스트 컴프리헨션과 조건",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Python 코드의 실행 결과를 쓰시오.

                        ```python
                        nums = [1, 2, 3, 4, 5, 6]
                        result = [x * 2 if x % 2 == 0 else x for x in nums]
                        print(result)
                        ```""",
                        "[1, 4, 3, 8, 5, 12]",
                        List.of("[1, 4, 3, 8, 5, 12]"),
                        "짝수면 2배, 홀수면 그대로. 1, 2*2=4, 3, 4*2=8, 5, 6*2=12.",
                        "리스트 컴프리헨션 + 삼항식",
                        3),
                new EngineerExample(
                        "Python - 클로저",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 Python 코드의 실행 결과를 쓰시오.

                        ```python
                        def make_counter(start):
                            count = [start]
                            def step():
                                count[0] += 1
                                return count[0]
                            return step

                        c = make_counter(10)
                        print(c(), c(), c())
                        ```""",
                        "11 12 13",
                        List.of("11 12 13"),
                        "클로저가 외부 함수의 count 리스트를 캡처. 호출마다 11, 12, 13으로 증가.",
                        "클로저 + 가변 캡처",
                        5)
        ));

        // ============================================================
        // SQL (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("SQL", List.of(
                new EngineerExample(
                        "SQL - RANK 윈도우 동순위",
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
                        "RANK()는 동순위가 있으면 다음 순위를 건너뛴다. 최=1, 이/박=공동2, 김=4.",
                        "RANK 윈도우 + 동순위",
                        5),
                new EngineerExample(
                        "SQL - GROUP BY 기본 집계",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 [주문] 테이블에서 SQL문을 실행한 결과로 출력되는 행의 개수를 쓰시오.

                        | 고객 | 상품 | 수량 |
                        |------|------|------|
                        | A | 사과 | 3 |
                        | B | 사과 | 2 |
                        | A | 배 | 1 |
                        | C | 배 | 4 |

                        ```sql
                        SELECT 상품, SUM(수량)
                        FROM 주문
                        GROUP BY 상품;
                        ```""",
                        "2",
                        List.of("2"),
                        "GROUP BY 상품 → 사과, 배 두 그룹으로 묶이므로 2행.",
                        "GROUP BY + SUM 행 수",
                        1),
                new EngineerExample(
                        "SQL - LEFT OUTER JOIN과 NULL",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 [사원]과 [부서] 테이블에서 SQL문 결과의 행 수를 쓰시오.

                        [사원]
                        | 이름 | 부서코드 |
                        | 김 | 10 |
                        | 이 | 20 |
                        | 박 | NULL |

                        [부서]
                        | 코드 | 부서명 |
                        | 10 | 영업 |
                        | 20 | 개발 |
                        | 30 | 인사 |

                        ```sql
                        SELECT 사원.이름, 부서.부서명
                        FROM 사원 LEFT OUTER JOIN 부서
                        ON 사원.부서코드 = 부서.코드;
                        ```""",
                        "3",
                        List.of("3"),
                        "LEFT OUTER JOIN은 좌측(사원) 모든 행 보존. 박은 부서코드 NULL이라 부서명 NULL로 매칭. 총 3행.",
                        "LEFT OUTER JOIN + NULL 보존",
                        3),
                new EngineerExample(
                        "SQL - 상관 서브쿼리",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 [급여] 테이블에서 SQL문을 실행한 결과로 출력되는 이름을 모두 쓰시오 (콤마로 구분).

                        | 이름 | 부서 | 급여 |
                        |------|------|------|
                        | 김 | A | 300 |
                        | 이 | A | 400 |
                        | 박 | B | 500 |
                        | 최 | B | 350 |

                        ```sql
                        SELECT 이름
                        FROM 급여 e1
                        WHERE 급여 > (SELECT AVG(급여) FROM 급여 e2 WHERE e2.부서 = e1.부서);
                        ```""",
                        "이, 박",
                        List.of("이, 박", "이,박"),
                        "부서별 평균: A=350, B=425. A에서 350 초과는 이(400). B에서 425 초과는 박(500).",
                        "상관 서브쿼리로 부서별 평균 비교",
                        5),
                new EngineerExample(
                        "SQL - LAG 윈도우 함수",
                        QuestionType.SHORT_ANSWER,
                        """
                        다음 [판매] 테이블에서 SQL문을 실행했을 때 월=4인 행의 prev 값을 쓰시오.

                        | 월 | 매출 |
                        |----|------|
                        | 1 | 100 |
                        | 2 | 150 |
                        | 3 | 130 |
                        | 4 | 200 |

                        ```sql
                        SELECT 월, 매출, LAG(매출, 1, 0) OVER (ORDER BY 월) AS prev
                        FROM 판매;
                        ```""",
                        "130",
                        List.of("130"),
                        "LAG는 이전 행의 값을 반환. 월=4의 이전 행은 월=3(매출 130).",
                        "LAG 윈도우 + 이전 행 참조",
                        3)
        ));

        // ============================================================
        // 소프트웨어 설계 (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("소프트웨어 설계", List.of(
                new EngineerExample(
                        "결합도와 응집도",
                        QuestionType.DESCRIPTIVE,
                        "소프트웨어 모듈의 **결합도(Coupling)**와 **응집도(Cohesion)**의 개념을 설명하고, 좋은 모듈 설계를 위해 각각 어떤 방향으로 설계해야 하는지 서술하시오.",
                        "결합도는 모듈 간 상호 의존 정도로 낮을수록 좋고, 응집도는 모듈 내부 요소가 하나의 기능을 위해 얼마나 밀접한지로 높을수록 좋다. 좋은 설계는 낮은 결합도와 높은 응집도를 추구하여 모듈 독립성과 유지보수성을 향상시킨다.",
                        List.of("결합도", "응집도", "낮은 결합도", "높은 응집도", "모듈 독립성"),
                        "결합도: 자료 → 스탬프 → 제어 → 외부 → 공통 → 내용. 응집도: 기능적 → 순차 → 통신 → 절차 → 시간 → 논리 → 우연.",
                        "결합도는 낮게, 응집도는 높게",
                        5),
                new EngineerExample(
                        "소프트웨어 개발 생명주기 모델",
                        QuestionType.SHORT_ANSWER,
                        "요구사항이 명확하고 변경 가능성이 낮을 때 적합하며, 분석→설계→구현→테스트→유지보수 단계가 순차적으로 진행되어 이전 단계로 돌아가기 어려운 소프트웨어 개발 생명주기 모델은 무엇인가?",
                        "폭포수 모델",
                        List.of("폭포수 모델", "폭포수", "Waterfall", "Waterfall Model"),
                        "Waterfall 모델은 가장 고전적인 SDLC로 단계 간 피드백이 어려운 단점이 있어, 요구사항 변경이 잦은 프로젝트에는 부적합하다.",
                        "SDLC 모델 식별",
                        1),
                new EngineerExample(
                        "UML 다이어그램 분류",
                        QuestionType.SHORT_ANSWER,
                        "UML 다이어그램은 시스템의 정적 구조를 표현하는 구조적 다이어그램과 시스템의 동작/흐름을 표현하는 행위적 다이어그램으로 나뉜다. 다음 중 행위적 다이어그램에 해당하는 것을 모두 고르시오.\n\n1) 클래스 다이어그램\n2) 유스케이스 다이어그램\n3) 객체 다이어그램\n4) 시퀀스 다이어그램\n5) 컴포넌트 다이어그램\n6) 활동 다이어그램",
                        "2, 4, 6",
                        List.of("2, 4, 6", "2,4,6", "유스케이스 다이어그램", "시퀀스 다이어그램", "활동 다이어그램"),
                        "구조적: 클래스, 객체, 컴포넌트, 배치, 패키지. 행위적: 유스케이스, 시퀀스, 활동, 상태, 협력, 통신, 타이밍.",
                        "UML 다이어그램 구조/행위 분류",
                        3),
                new EngineerExample(
                        "디자인 패턴 식별",
                        QuestionType.SHORT_ANSWER,
                        "객체 생성 로직을 별도의 클래스로 분리하여, 클라이언트가 구체 클래스를 알 필요 없이 인터페이스를 통해 객체를 생성하도록 하는 GoF 생성 패턴은 무엇인가?",
                        "팩토리 메소드 패턴",
                        List.of("팩토리 메소드 패턴", "팩토리 메서드", "Factory Method", "Factory Method Pattern"),
                        "팩토리 메소드 패턴은 객체 생성을 서브클래스에 위임하여 결합도를 낮춘다. 추상 팩토리(Abstract Factory)와 구분 필요.",
                        "GoF 생성 패턴 - Factory Method",
                        3),
                new EngineerExample(
                        "SOLID 원칙",
                        QuestionType.DESCRIPTIVE,
                        "객체지향 설계 원칙 중 **단일 책임 원칙(SRP: Single Responsibility Principle)**과 **개방-폐쇄 원칙(OCP: Open-Closed Principle)**의 의미를 각각 설명하시오.",
                        "SRP는 하나의 클래스가 하나의 책임만 가져야 한다는 원칙으로, 변경의 이유가 단 하나여야 한다. OCP는 소프트웨어 요소가 확장에는 열려 있고 변경에는 닫혀 있어야 한다는 원칙으로, 기존 코드 수정 없이 새 기능을 추가할 수 있어야 한다.",
                        List.of("단일 책임", "SRP", "개방-폐쇄", "OCP", "확장에 열려", "변경에 닫혀"),
                        "SOLID 원칙: SRP, OCP, LSP(리스코프 치환), ISP(인터페이스 분리), DIP(의존성 역전).",
                        "SOLID - SRP/OCP 정의",
                        5)
        ));

        // ============================================================
        // 데이터베이스 이론 (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("데이터베이스 이론", List.of(
                new EngineerExample(
                        "트랜잭션 ACID와 2PL",
                        QuestionType.DESCRIPTIVE,
                        "데이터베이스 트랜잭션의 4가지 특성(ACID)을 각각 설명하고, 동시성 제어 기법 중 **2단계 로킹 프로토콜(2PL)**의 원리를 서술하시오.",
                        "원자성은 트랜잭션이 모두 실행되거나 전혀 실행되지 않아야 하는 성질, 일관성은 트랜잭션 전후 DB가 일관된 상태를 유지하는 성질, 격리성은 동시 실행 트랜잭션이 서로 영향을 주지 않는 성질, 지속성은 완료된 결과가 영구적으로 반영되는 성질이다. 2PL은 확장 단계에서 Lock 획득, 축소 단계에서 Lock 해제만 허용하여 직렬 가능성을 보장한다.",
                        List.of("원자성", "일관성", "격리성", "지속성", "Atomicity", "확장 단계", "축소 단계"),
                        "2PL은 직렬 가능성을 보장하지만 교착 상태(Deadlock)가 발생할 수 있다.",
                        "ACID + 2PL 원리",
                        5),
                new EngineerExample(
                        "정규화 단계 식별 - 1NF",
                        QuestionType.SHORT_ANSWER,
                        "릴레이션의 모든 속성 값이 더 이상 분해될 수 없는 원자값(atomic value)으로만 구성되어 있는 정규형은 무엇인가?",
                        "제1정규형",
                        List.of("제1정규형", "1NF", "1정규형", "First Normal Form"),
                        "1NF는 모든 도메인이 원자값. 2NF는 부분 함수 종속 제거, 3NF는 이행 함수 종속 제거, BCNF는 결정자가 모두 후보키.",
                        "정규화 1NF 정의",
                        1),
                new EngineerExample(
                        "트랜잭션 격리 수준",
                        QuestionType.SHORT_ANSWER,
                        "트랜잭션이 커밋되지 않은 다른 트랜잭션의 변경 내용을 읽을 수 있어 Dirty Read 현상이 발생할 수 있는 가장 낮은 격리 수준은 무엇인가?",
                        "READ UNCOMMITTED",
                        List.of("READ UNCOMMITTED", "Read Uncommitted", "리드 언커밋티드"),
                        "격리 수준: READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE. 위로 갈수록 동시성 ↑, 일관성 ↓.",
                        "격리 수준 식별",
                        3),
                new EngineerExample(
                        "인덱스 종류",
                        QuestionType.SHORT_ANSWER,
                        "데이터 값의 카디널리티가 매우 낮은 컬럼(예: 성별, Y/N 플래그)에 적합하며, 각 값에 대해 비트 배열을 생성하여 AND/OR 연산으로 빠르게 검색하는 인덱스 종류는 무엇인가?",
                        "비트맵 인덱스",
                        List.of("비트맵 인덱스", "비트맵", "Bitmap Index", "Bitmap"),
                        "B-Tree 인덱스는 카디널리티가 높은 컬럼에 적합. 비트맵은 카디널리티가 낮을 때 압축률이 높고 OLAP에 자주 쓰임.",
                        "인덱스 종류 - 비트맵",
                        3),
                new EngineerExample(
                        "회복 기법",
                        QuestionType.DESCRIPTIVE,
                        "데이터베이스 회복 기법 중 **즉시 갱신(Immediate Update)** 방식과 **지연 갱신(Deferred Update)** 방식의 차이를 설명하고, 각각 회복 시 사용하는 로그 연산(Redo/Undo)을 서술하시오.",
                        "즉시 갱신은 트랜잭션 진행 중 변경사항을 즉시 DB에 반영하므로 회복 시 Redo와 Undo 모두 필요하다. 지연 갱신은 트랜잭션이 부분 완료될 때까지 변경사항을 로그에만 기록하고 커밋 후 DB에 반영하므로, 커밋 전 장애 시 Undo가 불필요하고 Redo만 사용한다.",
                        List.of("즉시 갱신", "지연 갱신", "Immediate Update", "Deferred Update", "Redo", "Undo"),
                        "즉시 갱신: Redo + Undo. 지연 갱신: Redo only. 그림자 페이징(Shadow Paging)은 로그 없이 회복 가능.",
                        "회복 기법 즉시/지연 갱신",
                        5)
        ));

        // ============================================================
        // 네트워크/OS (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("네트워크/OS", List.of(
                new EngineerExample(
                        "리눅스 chmod 8진수",
                        QuestionType.SHORT_ANSWER,
                        "리눅스에서 파일의 권한이 현재 `rw-r--r--`일 때, 소유자에게 실행 권한을 추가하고 그룹에게 쓰기 권한을 추가하는 `chmod` 명령어를 8진수 방식으로 작성하시오. (파일명은 test.sh)",
                        "chmod 764 test.sh",
                        List.of("chmod 764 test.sh", "764"),
                        "현재 644에서 소유자 +x → 7, 그룹 +w → 6, 기타 그대로 4. 따라서 764.",
                        "chmod 8진수 권한 계산",
                        5),
                new EngineerExample(
                        "OSI 7계층 식별",
                        QuestionType.SHORT_ANSWER,
                        "OSI 7계층 중 라우팅과 IP 주소 지정을 담당하며, 패킷의 발신지에서 목적지까지의 경로를 결정하는 계층은 무엇인가?",
                        "네트워크 계층",
                        List.of("네트워크 계층", "Network Layer", "3계층", "L3"),
                        "OSI: 1물리 - 2데이터링크 - 3네트워크 - 4전송 - 5세션 - 6표현 - 7응용. 네트워크 계층 대표 프로토콜은 IP, ICMP, ARP.",
                        "OSI 3계층 식별",
                        1),
                new EngineerExample(
                        "서브넷 마스킹",
                        QuestionType.SHORT_ANSWER,
                        "IP 주소 192.168.10.130/26 이 속한 네트워크 주소(Network Address)를 쓰시오.",
                        "192.168.10.128",
                        List.of("192.168.10.128"),
                        "/26은 서브넷 마스크 255.255.255.192로 마지막 옥텟을 64씩 분할. 130은 128~191 구간에 속하므로 네트워크 주소는 192.168.10.128.",
                        "CIDR /26 네트워크 주소 계산",
                        3),
                new EngineerExample(
                        "페이지 교체 알고리즘",
                        QuestionType.SHORT_ANSWER,
                        "참조 문자열이 `1, 2, 3, 4, 1, 2, 5, 1, 2, 3` 일 때, 페이지 프레임 3개를 가진 시스템에서 **LRU(Least Recently Used)** 알고리즘을 적용했을 때 발생하는 페이지 부재(Page Fault) 횟수를 쓰시오.",
                        "8",
                        List.of("8"),
                        "LRU 트레이싱: 1✗, 2✗, 3✗, 4✗(1제거), 1✗(2제거), 2✗(3제거), 5✗(4제거), 1○, 2○, 3✗(5제거). 부재 8회.",
                        "LRU 페이지 부재 계산",
                        3),
                new EngineerExample(
                        "TCP 헤더 플래그",
                        QuestionType.SHORT_ANSWER,
                        "TCP 헤더의 제어 비트(Control Flag) 중, 연결 설정 요청(3-way handshake의 첫 단계)에 사용되는 플래그는 무엇인가?",
                        "SYN",
                        List.of("SYN", "SYN 플래그", "Synchronize"),
                        "TCP 플래그: SYN(연결 요청), ACK(응답), FIN(연결 종료), RST(즉시 종료), PSH, URG. 3-way handshake = SYN → SYN+ACK → ACK.",
                        "TCP SYN 플래그 식별",
                        5)
        ));

        // ============================================================
        // 보안 (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("보안", List.of(
                new EngineerExample(
                        "SQL Injection 방어",
                        QuestionType.DESCRIPTIVE,
                        "**SQL 인젝션(SQL Injection)**의 공격 원리를 설명하고, 이를 방어하기 위한 대응 방안을 3가지 이상 서술하시오.",
                        "SQL 인젝션은 사용자 입력값을 검증하지 않아 공격자가 입력 필드에 악의적인 SQL 구문을 삽입하여 DB를 비정상적으로 조작하는 공격이다. 대응으로는 1) Prepared Statement(매개변수화 쿼리)로 입력과 SQL을 분리, 2) 입력값 검증과 특수문자 필터링, 3) 웹 방화벽(WAF) 도입, 4) DB 계정에 최소 권한 원칙 적용 등이 있다.",
                        List.of("SQL 구문 삽입", "Prepared Statement", "입력값 검증", "WAF", "최소 권한"),
                        "OWASP Top 10 대표 취약점. Prepared Statement는 SQL 구조를 미리 컴파일하므로 삽입된 SQL이 구문으로 해석되지 않는다.",
                        "SQL Injection 방어",
                        5),
                new EngineerExample(
                        "정보보안 3요소 (CIA)",
                        QuestionType.SHORT_ANSWER,
                        "정보보안의 3대 요소(CIA Triad) 중, 인가받지 않은 사용자가 정보에 접근하지 못하도록 보호하는 특성은 무엇인가?",
                        "기밀성",
                        List.of("기밀성", "Confidentiality", "Confidentiality 기밀성"),
                        "CIA: 기밀성(Confidentiality), 무결성(Integrity), 가용성(Availability). 기밀성은 암호화·접근통제로 확보.",
                        "CIA 트라이어드 - 기밀성",
                        1),
                new EngineerExample(
                        "대칭 vs 비대칭 암호화",
                        QuestionType.SHORT_ANSWER,
                        "암호화와 복호화에 동일한 키를 사용하며, 처리 속도가 빠르지만 키 분배 문제가 있는 암호화 방식은 무엇인가? 또한 이 방식의 대표 알고리즘 1개를 함께 쓰시오.",
                        "대칭키 암호화, AES",
                        List.of("대칭키 암호화", "대칭키", "AES", "DES", "Symmetric"),
                        "대칭키: AES, DES, 3DES, SEED, ARIA. 비대칭키: RSA, ECC, ElGamal. 대칭키는 빠르지만 키 분배 문제, 비대칭키는 느리지만 키 분배 안전.",
                        "대칭키 암호화 + 알고리즘",
                        3),
                new EngineerExample(
                        "해시 함수의 특성",
                        QuestionType.SHORT_ANSWER,
                        "암호학적 해시 함수가 가져야 할 핵심 특성 중, 같은 해시값을 가지는 서로 다른 두 입력을 찾는 것이 계산적으로 불가능해야 한다는 특성은 무엇인가?",
                        "충돌 저항성",
                        List.of("충돌 저항성", "충돌 회피성", "Collision Resistance", "Collision-Free"),
                        "해시 함수 특성: 일방향성(역상 저항성), 제2 역상 저항성, 충돌 저항성. SHA-1은 충돌 저항성이 깨져 deprecated.",
                        "해시 충돌 저항성 정의",
                        3),
                new EngineerExample(
                        "접근 통제 모델",
                        QuestionType.SHORT_ANSWER,
                        "주체의 역할(Role)에 권한을 부여하고, 사용자에게 역할을 할당함으로써 권한을 관리하는 접근 통제 모델은 무엇인가?",
                        "RBAC",
                        List.of("RBAC", "역할 기반 접근 통제", "Role-Based Access Control"),
                        "DAC(임의적), MAC(강제적), RBAC(역할 기반). RBAC는 대규모 조직의 권한 관리에 효율적.",
                        "RBAC 접근통제 모델",
                        5)
        ));

        // ============================================================
        // 자료구조/알고리즘 (3개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("자료구조/알고리즘", List.of(
                new EngineerExample(
                        "스택과 큐",
                        QuestionType.SHORT_ANSWER,
                        "다음은 스택(Stack)에 1, 2, 3, 4, 5를 순서대로 push한 후, pop을 3번 수행했을 때 마지막으로 꺼낸 값을 쓰시오.",
                        "3",
                        List.of("3"),
                        "스택은 LIFO(Last In First Out) 구조이다. push(1,2,3,4,5) 후 pop 3번 → 5, 4, 3 순으로 꺼내므로 마지막으로 꺼낸 값은 3이다.",
                        "스택 LIFO 구조에서 pop 순서를 묻는 기본 문제",
                        1),
                new EngineerExample(
                        "정렬 알고리즘",
                        QuestionType.SHORT_ANSWER,
                        "다음 중 평균 시간 복잡도가 O(n²)인 정렬 알고리즘을 모두 쓰시오.\n\n- 퀵 정렬\n- 버블 정렬\n- 선택 정렬\n- 병합 정렬",
                        "버블 정렬, 선택 정렬",
                        List.of("버블 정렬", "선택 정렬", "버블정렬", "선택정렬"),
                        "버블 정렬과 선택 정렬의 평균 시간 복잡도는 O(n²)이다. 퀵 정렬은 평균 O(n log n), 병합 정렬은 항상 O(n log n)이다.",
                        "정렬 알고리즘 시간 복잡도 비교",
                        2),
                new EngineerExample(
                        "트리 순회",
                        QuestionType.SHORT_ANSWER,
                        "다음 이진 트리를 전위 순회(Preorder Traversal)한 결과를 쓰시오.\n\n        A\n       / \\\n      B   C\n     / \\\n    D   E",
                        "A B D E C",
                        List.of("A B D E C", "ABDEC"),
                        "전위 순회는 루트→왼쪽→오른쪽 순서로 방문한다. A → B → D → E → C 순이다.",
                        "이진 트리 전위 순회 결과를 추적하는 문제",
                        2)));
        // ============================================================
        // 신기술 동향 (5개)
        // ============================================================
        EXAMPLES_BY_CATEGORY.put("신기술 동향", List.of(
                new EngineerExample(
                        "컨테이너와 마이크로서비스",
                        QuestionType.DESCRIPTIVE,
                        "**컨테이너(Container)** 기술과 **마이크로서비스 아키텍처(MSA)**의 개념을 각각 설명하고, 두 기술이 결합되어 사용되는 이유를 서술하시오.",
                        "컨테이너는 애플리케이션과 실행 환경을 패키지로 격리하여 어디서든 동일하게 실행하는 경량 가상화 기술이다. MSA는 애플리케이션을 작고 독립적인 서비스 단위로 분리하여 각 서비스가 독립 배포·확장될 수 있도록 하는 설계 방식이다. 컨테이너는 각 마이크로서비스의 최적 실행 단위를 제공하고, 쿠버네티스 같은 오케스트레이션으로 대규모 관리를 가능하게 하기에 결합되어 사용된다.",
                        List.of("컨테이너", "경량 가상화", "마이크로서비스", "독립 배포", "Docker", "쿠버네티스"),
                        "Docker가 대표적이며, OS 커널 공유로 VM보다 가볍다. 쿠버네티스는 컨테이너 오케스트레이션 플랫폼.",
                        "컨테이너 + MSA 결합",
                        5),
                new EngineerExample(
                        "클라우드 서비스 모델",
                        QuestionType.SHORT_ANSWER,
                        "클라우드 서비스 모델 중, 사용자가 인프라(서버·스토리지·네트워크)는 신경 쓰지 않고 애플리케이션 개발과 실행 환경(런타임·미들웨어)만 제공받는 모델은 무엇인가?",
                        "PaaS",
                        List.of("PaaS", "Platform as a Service", "플랫폼 서비스"),
                        "IaaS(인프라), PaaS(플랫폼), SaaS(소프트웨어). PaaS 예: Heroku, Google App Engine, AWS Elastic Beanstalk.",
                        "클라우드 PaaS 식별",
                        1),
                new EngineerExample(
                        "인공지능 학습 분류",
                        QuestionType.SHORT_ANSWER,
                        "인공지능 학습 방식 중, 정답(label)이 없는 데이터에서 패턴이나 군집을 스스로 발견하는 학습 방식은 무엇인가?",
                        "비지도 학습",
                        List.of("비지도 학습", "Unsupervised Learning", "비지도학습"),
                        "지도 학습(라벨 있음, 분류·회귀), 비지도 학습(라벨 없음, 군집화·차원 축소), 강화 학습(보상 기반).",
                        "비지도 학습 정의",
                        3),
                new EngineerExample(
                        "블록체인 합의 알고리즘",
                        QuestionType.SHORT_ANSWER,
                        "블록체인의 합의 알고리즘 중, 노드가 보유한 코인의 양과 보유 기간에 비례해 블록 생성 권한을 부여하는 방식으로 PoW의 에너지 소비 문제를 개선한 알고리즘은 무엇인가?",
                        "지분 증명",
                        List.of("지분 증명", "PoS", "Proof of Stake", "Proof-of-Stake"),
                        "PoW(작업증명, 비트코인), PoS(지분증명, 이더리움 2.0), DPoS(위임 지분증명), PBFT.",
                        "PoS 합의 알고리즘",
                        3),
                new EngineerExample(
                        "DevOps와 CI/CD",
                        QuestionType.DESCRIPTIVE,
                        "**DevOps**의 개념을 설명하고, DevOps 실현의 핵심 기법인 **CI(Continuous Integration)** 와 **CD(Continuous Delivery/Deployment)** 의 차이를 서술하시오.",
                        "DevOps는 개발(Development)과 운영(Operations)의 협업과 자동화를 통해 소프트웨어 배포 주기를 단축하고 품질을 향상시키는 문화·실천 방법이다. CI는 개발자의 코드 변경을 자주 통합하고 자동화된 빌드·테스트로 통합 오류를 조기에 발견하는 기법이다. CD는 통합된 코드를 자동으로 배포 가능한 상태로 유지(Continuous Delivery)하거나 운영 환경에 자동 배포(Continuous Deployment)하는 기법이다.",
                        List.of("DevOps", "개발", "운영", "CI", "Continuous Integration", "CD", "Continuous Delivery", "자동화"),
                        "Jenkins, GitHub Actions, GitLab CI/CD 등이 대표 도구. Continuous Delivery는 수동 승인 후 배포, Continuous Deployment는 완전 자동 배포.",
                        "DevOps + CI/CD",
                        5)
        ));
    }

    /** 카테고리 시드 풀에서 needed 개수만큼 무작위 추출 (중복 없이, 풀이 작으면 가용한 만큼만) */
    public static List<EngineerExample> randomFor(String category, int needed, Random random) {
        List<EngineerExample> pool = EXAMPLES_BY_CATEGORY.get(category);
        if (pool == null || pool.isEmpty()) return List.of();

        List<EngineerExample> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);

        if (needed >= shuffled.size()) {
            // 부족하면 가용한 만큼만 (중복 허용 X)
            return new ArrayList<>(shuffled);
        }
        return new ArrayList<>(shuffled.subList(0, needed));
    }

    /** 코드 블록 안의 식별자(함수/클래스/변수명) 추출 — anti-example용 */
    private static final Pattern CODE_FENCE = Pattern.compile("```[a-zA-Z]*\\s*([\\s\\S]*?)```");
    private static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{2,}");
    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList(
            // C/Java/Python/SQL 공통 예약어 + 흔한 라이브러리 식별자
            "int", "void", "char", "long", "short", "double", "float", "bool", "true", "false", "null",
            "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return", "default",
            "class", "interface", "abstract", "extends", "implements", "static", "final", "public", "private",
            "protected", "new", "this", "super", "import", "package", "throws", "throw", "try", "catch", "finally",
            "def", "lambda", "yield", "elif", "and", "or", "not", "in", "is", "None", "True", "False", "self",
            "print", "println", "printf", "input", "len", "range", "list", "dict", "tuple", "set", "str",
            "include", "stdio", "main", "sizeof", "struct", "typedef", "enum", "union", "const", "extern",
            "select", "from", "where", "group", "order", "having", "join", "inner", "outer", "left", "right",
            "union", "all", "distinct", "asc", "desc", "limit", "offset", "into", "values", "table", "view",
            "primary", "foreign", "key", "index", "create", "drop", "alter", "update", "delete", "insert",
            "System", "out", "String", "Integer", "Double", "Float", "Object", "Boolean", "Math",
            "Arrays", "Collections", "ArrayList", "HashMap", "HashSet", "List", "Map", "Set", "Stream",
            "args", "stdin", "stdout", "stderr", "size", "length", "value", "result", "data", "item", "items",
            "java", "util", "lang", "io", "stream", "text", "func", "fun"
    ));

    public static List<String> identifiersOf(EngineerExample example) {
        if (example == null) return List.of();
        Set<String> found = new java.util.LinkedHashSet<>();
        Matcher fenceMatcher = CODE_FENCE.matcher(example.content());
        while (fenceMatcher.find()) {
            String code = fenceMatcher.group(1);
            Matcher idMatcher = IDENT.matcher(code);
            while (idMatcher.find()) {
                String ident = idMatcher.group();
                if (!RESERVED.contains(ident) && !RESERVED.contains(ident.toLowerCase())) {
                    found.add(ident);
                }
            }
        }
        return new ArrayList<>(found).subList(0, Math.min(found.size(), 12));
    }

    /** 여러 시드에서 식별자를 모두 모아 dedupe (최대 20개) */
    public static List<String> identifiersOfAll(List<EngineerExample> examples) {
        Set<String> all = new java.util.LinkedHashSet<>();
        for (EngineerExample ex : examples) {
            all.addAll(identifiersOf(ex));
        }
        return new ArrayList<>(all).subList(0, Math.min(all.size(), 20));
    }

    private EngineerTopicExamples() {
    }
}
