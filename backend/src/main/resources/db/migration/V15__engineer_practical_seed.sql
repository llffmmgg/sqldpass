-- 정보처리기사 실기 샘플 문제 시드 (자동 생성)
-- Source: scripts/generate-engineer-seed.mjs
-- 총 27문항

-- id=1 C언어 / C - 제어문과 반복문
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'C언어' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 C 코드의 실행 결과를 쓰시오.

```c
#include <stdio.h>
int main() {
    int i, sum = 0;
    for (i = 1; i <= 5; i++) {
        sum += i;
    }
    printf("%d\\n", sum);
    return 0;
}
```', 'SHORT_ANSWER', NULL, '15', '["15"]', '`i`가 1부터 5까지 반복하며 `sum`에 누적한다. 1+2+3+4+5 = 15가 출력된다.', '단순 for 반복문 누적합', 'C - 제어문과 반복문', 1, NOW(6), NOW(6));

-- id=2 C언어 / C - 포인터와 배열
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'C언어' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 C 코드의 실행 결과를 쓰시오.

```c
#include <stdio.h>
int main() {
    int arr[] = {10, 20, 30, 40, 50};
    int *p = arr + 1;
    printf("%d, %d\\n", *(p + 2), p[-1]);
    return 0;
}
```', 'SHORT_ANSWER', NULL, '40, 10', '["40","10"]', '`p = arr + 1`이므로 `p`는 `arr[1]`(값 20)을 가리킨다. `*(p + 2)`는 `arr[3]` = 40이다. `p[-1]`은 `*(p - 1)` = `arr[0]` = 10이다.', '포인터 산술과 음수 인덱스', 'C - 포인터와 배열', 3, NOW(6), NOW(6));

-- id=3 C언어 / C - 재귀함수와 구조체
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'C언어' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 C 코드의 실행 결과를 쓰시오.

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
```', 'SHORT_ANSWER', NULL, '2 4 1 6 1 3', '["2 4 1 6 1 3"]', 'func(6): func(4) → print 6 → func(3)
  func(4): func(2) → print 4 → func(1)
    func(2): func(0) → print 2 → func(-1) → 출력: 2
    print 4 → 출력: 4
    func(1): func(-1) → print 1 → func(-2) → 출력: 1
  print 6 → 출력: 6
  func(3): func(1) → print 3 → func(0)
    func(1): func(-1) → print 1 → func(-2) → 출력: 1
    print 3 → 출력: 3
최종 출력: 2 4 1 6 1 3', '이중 재귀 호출 트레이싱', 'C - 재귀함수와 구조체', 5, NOW(6), NOW(6));

-- id=4 Java / Java - 문자열 메서드
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'Java' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 Java 코드의 실행 결과를 쓰시오.

```java
public class Main {
    public static void main(String[] args) {
        String s = "HelloWorld";
        System.out.println(s.substring(5).toUpperCase());
    }
}
```', 'SHORT_ANSWER', NULL, 'WORLD', '["WORLD"]', '`s.substring(5)`는 인덱스 5부터 끝까지 추출하여 `"World"`를 반환한다. `.toUpperCase()`로 대문자 변환하면 `"WORLD"`가 된다.', 'substring + toUpperCase 체이닝', 'Java - 문자열 메서드', 1, NOW(6), NOW(6));

-- id=5 Java / Java - 상속과 오버라이딩
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'Java' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 Java 코드의 실행 결과를 쓰시오.

```java
class A {
    int x = 10;
    int getX() { return x; }
}
class B extends A {
    int x = 20;
    int getX() { return x; }
}
public class Main {
    public static void main(String[] args) {
        A a = new B();
        System.out.println(a.x + " " + a.getX());
    }
}
```', 'SHORT_ANSWER', NULL, '10 20', '["10","20"]', 'Java에서 **필드**는 참조 변수의 선언 타입(A)을 기준으로 정적 바인딩되므로 `a.x`는 A의 x인 10이다. **메서드**는 실제 객체(B)를 기준으로 동적 디스패치되므로 `a.getX()`는 B의 `getX()`가 호출되어 20을 반환한다.', '필드는 정적 바인딩, 메서드는 동적 바인딩', 'Java - 상속과 오버라이딩', 3, NOW(6), NOW(6));

-- id=6 Java / Java - 추상클래스와 인터페이스
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'Java' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 Java 코드의 실행 결과를 쓰시오.

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
```', 'SHORT_ANSWER', NULL, 'Shape Rect 12', '["Shape","Rect","12"]', '`s.type`은 선언 타입 Shape의 필드이므로 `"Shape"`. `s.getType()`은 오버라이딩된 Rect의 메서드가 호출되어 `"Rect"`. `s.area()`는 Rect의 area()가 호출되어 3×4 = 12. 필드 은닉(hiding)과 메서드 오버라이딩의 차이를 묻는 문제이다.', '필드 은닉 vs 메서드 오버라이딩 + 추상클래스', 'Java - 추상클래스와 인터페이스', 5, NOW(6), NOW(6));

-- id=7 Python / Python - 리스트 기본 연산
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'Python' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 Python 코드의 실행 결과를 쓰시오.

```python
a = [1, 2, 3, 4, 5]
print(a[1:4])
```', 'SHORT_ANSWER', NULL, '[2, 3, 4]', '["[2, 3, 4]"]', '슬라이싱 `a[1:4]`는 인덱스 1부터 3까지(4 미포함)의 원소를 추출한다. 따라서 `[2, 3, 4]`가 출력된다.', '리스트 슬라이싱 기본', 'Python - 리스트 기본 연산', 1, NOW(6), NOW(6));

-- id=8 Python / Python - 딕셔너리와 컴프리헨션
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'Python' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 Python 코드의 실행 결과를 쓰시오.

```python
data = {\'A\': 3, \'B\': 1, \'C\': 4, \'D\': 1}
result = sorted(data.items(), key=lambda x: x[1])
print([k for k, v in result])
```', 'SHORT_ANSWER', NULL, '[\'B\', \'D\', \'A\', \'C\']', '["B","D","A","C"]', '`data.items()`를 value 기준으로 정렬하면 `[(\'B\',1), (\'D\',1), (\'A\',3), (\'C\',4)]`이 된다. 같은 값(1)인 B와 D는 원래 딕셔너리 삽입 순서가 유지된다(Python 3.7+ 안정 정렬). key만 추출하면 `[\'B\', \'D\', \'A\', \'C\']`이다.', '딕셔너리 정렬과 안정 정렬', 'Python - 딕셔너리와 컴프리헨션', 3, NOW(6), NOW(6));

-- id=9 Python / Python - 클래스와 매직 메서드
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'Python' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음 Python 코드의 실행 결과를 쓰시오.

```python
def func(x, y=[]):
    y.append(x)
    return y

print(func(1))
print(func(2))
print(func(3, []))
print(func(4))
```', 'SHORT_ANSWER', NULL, '[1]
[1, 2]
[3]
[1, 2, 4]', '["[1]","[1, 2]","[3]","[1, 2, 4]"]', 'Python에서 **가변 기본 인자**는 함수 정의 시 단 한 번만 생성된다. `func(1)` → 기본 리스트에 1 추가 → `[1]`. `func(2)` → 같은 리스트에 2 추가 → `[1, 2]`. `func(3, [])` → 새 리스트 전달 → `[3]` (기본 리스트에 영향 없음). `func(4)` → 다시 기본 리스트에 4 추가 → `[1, 2, 4]`. 이는 Python의 대표적인 함정(mutable default argument)이다.', '가변 기본 인자 함정 (mutable default argument)', 'Python - 클래스와 매직 메서드', 5, NOW(6), NOW(6));

-- id=10 SQL / SQL - 기본 SELECT
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'SQL' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음과 같은 [학생] 테이블이 있을 때, SQL문의 실행 결과를 쓰시오.

| 학번 | 이름 | 학과 | 학년 |
|------|------|------|------|
| 101 | 김철수 | 컴퓨터 | 3 |
| 102 | 이영희 | 전자 | 2 |
| 103 | 박민수 | 컴퓨터 | 4 |
| 104 | 최지연 | 컴퓨터 | 2 |

```sql
SELECT COUNT(*) FROM 학생 WHERE 학과 = \'컴퓨터\';
```', 'SHORT_ANSWER', NULL, '3', '["3"]', '학과가 \'컴퓨터\'인 행은 101, 103, 104로 총 3개이다. `COUNT(*)`는 해당 조건을 만족하는 행의 수를 반환하므로 결과는 3이다.', 'WHERE 조건과 COUNT 집계함수', 'SQL - 기본 SELECT', 1, NOW(6), NOW(6));

-- id=11 SQL / SQL - JOIN과 서브쿼리
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'SQL' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음과 같은 테이블이 있을 때, SQL문의 실행 결과를 쓰시오.

[사원]
| 사번 | 이름 | 부서코드 |
|------|------|----------|
| 1 | 김 | D1 |
| 2 | 이 | D2 |
| 3 | 박 | D1 |
| 4 | 최 | D3 |

[부서]
| 부서코드 | 부서명 |
|----------|--------|
| D1 | 개발 |
| D2 | 기획 |
| D3 | 인사 |

```sql
SELECT B.부서명, COUNT(*) AS 인원
FROM 사원 A JOIN 부서 B ON A.부서코드 = B.부서코드
GROUP BY B.부서명
HAVING COUNT(*) >= 2;
```', 'SHORT_ANSWER', NULL, '개발 2', '["개발","2"]', '사원과 부서를 JOIN하면 D1(개발)에 2명, D2(기획)에 1명, D3(인사)에 1명이 된다. `HAVING COUNT(*) >= 2` 조건에 의해 개발 부서만 남으며 인원은 2이다.', 'JOIN + GROUP BY + HAVING', 'SQL - JOIN과 서브쿼리', 3, NOW(6), NOW(6));

-- id=12 SQL / SQL - 윈도우 함수
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = 'SQL' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '다음과 같은 [매출] 테이블이 있을 때, SQL문의 실행 결과에서 이름이 \'이\'인 행의 순위(rk) 값을 쓰시오.

| 이름 | 부서 | 금액 |
|------|------|------|
| 김 | A | 100 |
| 이 | A | 200 |
| 박 | A | 200 |
| 최 | B | 300 |

```sql
SELECT 이름, RANK() OVER (ORDER BY 금액 DESC) AS rk
FROM 매출;
```', 'SHORT_ANSWER', NULL, '2', '["2"]', '`RANK()`는 금액 내림차순으로 순위를 매긴다. 최(300) → 1위, 이(200)와 박(200) → 공동 2위, 김(100) → 4위(3위를 건너뜀). 이름이 \'이\'인 행의 rk는 2이다. `RANK()`는 동순위가 있으면 다음 순위를 건너뛰는 특성이 있다.', 'RANK 윈도우 함수와 동순위 처리', 'SQL - 윈도우 함수', 5, NOW(6), NOW(6));

-- id=13 소프트웨어 설계 / UML 다이어그램
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '소프트웨어 설계' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), 'UML(Unified Modeling Language)에서 시스템과 사용자(액터) 간의 상호작용을 표현하며, 시스템이 제공하는 기능을 사용자 관점에서 나타내는 다이어그램의 이름을 쓰시오.', 'SHORT_ANSWER', NULL, '유스케이스 다이어그램', '["유스케이스","Use Case"]', '**유스케이스 다이어그램(Use Case Diagram)**은 시스템의 기능적 요구사항을 액터와 유스케이스 간의 관계로 표현한다. 액터는 시스템 외부에서 시스템과 상호작용하는 사람 또는 외부 시스템이며, 유스케이스는 시스템이 수행하는 기능 단위이다.', '유스케이스 다이어그램 = 액터 + 기능 관계', 'UML 다이어그램', 1, NOW(6), NOW(6));

-- id=14 소프트웨어 설계 / 디자인 패턴
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '소프트웨어 설계' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), 'GoF 디자인 패턴 중, 객체의 인스턴스가 오직 하나만 생성되도록 보장하고, 어디서든 그 인스턴스에 접근할 수 있도록 전역적인 접근점을 제공하는 생성 패턴의 이름을 쓰시오.', 'SHORT_ANSWER', NULL, '싱글톤 패턴', '["싱글톤","Singleton"]', '**싱글톤 패턴(Singleton Pattern)**은 클래스의 인스턴스를 하나만 생성하고, 이에 대한 전역 접근점을 제공하는 생성 패턴이다. 생성자를 private으로 선언하고 정적 메서드를 통해 유일한 인스턴스를 반환한다. 데이터베이스 연결 풀, 로그 관리자 등에 주로 사용된다.', '인스턴스 하나만 보장하는 생성 패턴', '디자인 패턴', 3, NOW(6), NOW(6));

-- id=15 소프트웨어 설계 / 결합도와 응집도
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '소프트웨어 설계' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '소프트웨어 모듈의 **결합도(Coupling)**와 **응집도(Cohesion)**의 개념을 설명하고, 좋은 모듈 설계를 위해 각각 어떤 방향으로 설계해야 하는지 서술하시오.', 'DESCRIPTIVE', NULL, '결합도는 모듈 간의 상호 의존 정도를 나타내며, 낮을수록 좋다. 응집도는 모듈 내부 요소들이 하나의 기능을 위해 얼마나 밀접하게 관련되어 있는지를 나타내며, 높을수록 좋다. 좋은 모듈 설계란 낮은 결합도(Low Coupling)와 높은 응집도(High Cohesion)를 가지는 것으로, 모듈의 독립성을 높여 유지보수성과 재사용성을 향상시킨다.', '["결합도","응집도","낮은 결합도","높은 응집도","모듈 독립성","유지보수성"]', '결합도의 유형은 자료 결합도(가장 낮음) → 스탬프 → 제어 → 외부 → 공통 → 내용 결합도(가장 높음) 순이다. 응집도의 유형은 기능적 응집도(가장 높음) → 순차적 → 통신적 → 절차적 → 시간적 → 논리적 → 우연적 응집도(가장 낮음) 순이다. 이상적인 설계는 모듈 간에는 자료 결합도 수준으로 느슨하게, 모듈 내부는 기능적 응집도 수준으로 강하게 묶는 것이다.', '결합도는 낮게, 응집도는 높게 = 좋은 모듈 설계', '결합도와 응집도', 5, NOW(6), NOW(6));

-- id=16 데이터베이스 이론 / 키의 종류
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '데이터베이스 이론' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '데이터베이스에서 릴레이션의 각 튜플을 유일하게 식별할 수 있는 속성 또는 속성의 집합으로, 유일성과 최소성을 모두 만족하는 키의 이름을 쓰시오.', 'SHORT_ANSWER', NULL, '후보키', '["후보키","Candidate Key"]', '**후보키(Candidate Key)**는 릴레이션에서 각 튜플을 유일하게 식별할 수 있는 속성의 최소 집합이다. 유일성(Uniqueness)과 최소성(Minimality)을 모두 만족해야 한다. 후보키 중 하나를 선택하면 기본키(Primary Key)가 되고, 나머지는 대체키(Alternate Key)가 된다.', '유일성 + 최소성 = 후보키', '키의 종류', 1, NOW(6), NOW(6));

-- id=17 데이터베이스 이론 / 정규화
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '데이터베이스 이론' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '데이터베이스 정규화에서 **제1정규형(1NF)**, **제2정규형(2NF)**, **제3정규형(3NF)**의 조건을 각각 간략히 설명하시오.', 'DESCRIPTIVE', NULL, '제1정규형(1NF)은 릴레이션의 모든 속성 값이 원자값(Atomic Value)으로만 구성된 상태이다. 제2정규형(2NF)은 1NF를 만족하면서 부분 함수 종속을 제거하여 모든 비주요 속성이 기본키에 완전 함수 종속되는 상태이다. 제3정규형(3NF)은 2NF를 만족하면서 이행적 함수 종속을 제거하여 비주요 속성이 기본키에 직접 종속되는 상태이다.', '["원자값","부분 함수 종속 제거","완전 함수 종속","이행적 함수 종속 제거","1NF","2NF","3NF"]', '1NF: 반복 그룹이나 다중값 속성을 제거하여 모든 값을 원자값으로 만든다. 2NF: 복합키의 일부에만 종속되는 부분 함수 종속을 제거한다. 예를 들어 기본키가 (학번, 과목코드)일 때 학생이름이 학번에만 종속되면 부분 종속이다. 3NF: A→B→C와 같은 이행적 종속을 제거한다. 예를 들어 학번→학과→학과장이면 학과장은 학번에 이행적으로 종속된 것이다.', '1NF: 원자값, 2NF: 부분종속 제거, 3NF: 이행종속 제거', '정규화', 3, NOW(6), NOW(6));

-- id=18 데이터베이스 이론 / 트랜잭션 ACID
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '데이터베이스 이론' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '데이터베이스 트랜잭션의 4가지 특성(ACID)을 각각 설명하고, 동시성 제어 기법 중 **2단계 로킹 프로토콜(2PL)**의 원리를 서술하시오.', 'DESCRIPTIVE', NULL, '원자성(Atomicity)은 트랜잭션의 연산이 모두 실행되거나 모두 실행되지 않아야 하는 성질이다. 일관성(Consistency)은 트랜잭션 전후로 데이터베이스가 일관된 상태를 유지해야 하는 성질이다. 격리성(Isolation)은 동시에 실행되는 트랜잭션들이 서로 영향을 미치지 않아야 하는 성질이다. 지속성(Durability)은 성공적으로 완료된 트랜잭션의 결과가 영구적으로 반영되어야 하는 성질이다. 2단계 로킹 프로토콜은 확장 단계에서만 Lock을 획득하고, 축소 단계에서만 Lock을 해제하여 직렬 가능성을 보장하는 동시성 제어 기법이다.', '["원자성","일관성","격리성","지속성","Atomicity","Consistency","Isolation","Durability","확장 단계","축소 단계","직렬 가능성"]', 'ACID는 트랜잭션의 안전성을 보장하는 4대 특성이다. 2PL(Two-Phase Locking)은 트랜잭션을 **확장 단계(Growing Phase)**와 **축소 단계(Shrinking Phase)**로 나눈다. 확장 단계에서는 Lock만 획득할 수 있고, 축소 단계에서는 Lock만 해제할 수 있다. 한번 Lock을 해제하면 새로운 Lock을 획득할 수 없다. 이를 통해 직렬 가능한 스케줄을 보장하지만, 교착 상태(Deadlock)가 발생할 수 있다는 단점이 있다.', 'ACID 4특성 + 2PL은 확장/축소 단계로 직렬 가능성 보장', '트랜잭션 ACID', 5, NOW(6), NOW(6));

-- id=19 네트워크/OS / OSI 7계층
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '네트워크/OS' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), 'OSI 7계층 모델에서 데이터 링크 계층과 네트워크 계층 사이에 위치하지 않는 계층이며, 종단 간(End-to-End) 신뢰성 있는 데이터 전송을 담당하는 계층의 이름을 쓰시오.', 'SHORT_ANSWER', NULL, '전송 계층', '["전송 계층","Transport Layer","4계층"]', '**전송 계층(Transport Layer, 4계층)**은 종단 간 신뢰성 있는 데이터 전송을 담당한다. 대표 프로토콜로 TCP(연결 지향, 신뢰성)와 UDP(비연결, 비신뢰성)가 있다. 포트 번호를 사용하여 프로세스 간 통신을 구별한다.', '종단 간 신뢰성 전송 = 전송 계층', 'OSI 7계층', 1, NOW(6), NOW(6));

-- id=20 네트워크/OS / 프로세스 스케줄링
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '네트워크/OS' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '운영체제의 프로세스 스케줄링 알고리즘 중 **SJF(Shortest Job First)**와 **RR(Round Robin)**의 동작 원리를 각각 설명하고, 차이점을 서술하시오.', 'DESCRIPTIVE', NULL, 'SJF는 실행 시간이 가장 짧은 프로세스에게 CPU를 먼저 할당하는 비선점형 알고리즘으로, 평균 대기 시간을 최소화하지만 긴 프로세스가 계속 밀리는 기아(Starvation) 문제가 발생할 수 있다. RR은 각 프로세스에게 동일한 시간 할당량(Time Quantum)을 부여하고 순환적으로 CPU를 할당하는 선점형 알고리즘으로, 공정한 CPU 배분이 가능하지만 시간 할당량이 너무 크면 FCFS와 같아지고 너무 작으면 문맥 교환 오버헤드가 증가한다.', '["SJF","최단 작업 우선","비선점","기아","RR","시간 할당량","선점형","문맥 교환"]', 'SJF는 비선점형으로 현재 실행 중인 프로세스가 끝나야 다음 프로세스를 선택한다. 최적의 평균 대기 시간을 보장하지만, 실행 시간이 긴 프로세스가 무한 대기하는 **기아(Starvation)** 문제가 있다. SJF의 선점형 버전은 SRTF(Shortest Remaining Time First)이다. RR은 모든 프로세스가 공평하게 CPU를 사용하므로 대화형 시스템에 적합하다. Time Quantum의 크기가 성능에 큰 영향을 미친다.', 'SJF: 짧은 작업 우선/비선점, RR: 시간 할당량 순환/선점', '프로세스 스케줄링', 3, NOW(6), NOW(6));

-- id=21 네트워크/OS / 리눅스 명령어와 권한
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '네트워크/OS' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '리눅스에서 파일의 권한이 현재 `rw-r--r--`일 때, 소유자에게 실행 권한을 추가하고 그룹에게 쓰기 권한을 추가하는 `chmod` 명령어를 8진수 방식으로 작성하시오. (파일명은 test.sh라고 가정)', 'SHORT_ANSWER', NULL, 'chmod 764 test.sh', '["chmod","764"]', '현재 권한 `rw-r--r--`은 8진수로 644이다. 소유자에게 실행 권한(x) 추가 → `rwx` = 7. 그룹에게 쓰기 권한(w) 추가 → `rw-` = 6. 기타 사용자는 변경 없이 `r--` = 4. 따라서 `chmod 764 test.sh`가 된다. 각 자리는 r(4)+w(2)+x(1)의 합으로 계산된다.', 'chmod 8진수 권한 설정 계산', '리눅스 명령어와 권한', 5, NOW(6), NOW(6));

-- id=22 보안 / 보안 용어 - 공격 유형
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '보안' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '네트워크 보안에서, 공격자가 정상 사용자로 위장하여 패킷의 출발지 IP 주소를 변조하는 공격 기법의 이름을 쓰시오.', 'SHORT_ANSWER', NULL, 'IP 스푸핑', '["스푸핑","Spoofing","IP Spoofing"]', '**IP 스푸핑(IP Spoofing)**은 공격자가 자신의 IP 주소를 신뢰할 수 있는 다른 IP 주소로 위조하여 패킷을 전송하는 공격이다. 이를 통해 인증을 우회하거나 출처를 숨길 수 있다. ARP 스푸핑, DNS 스푸핑 등 다양한 변형이 있다.', '출발지 IP 변조 = IP 스푸핑', '보안 용어 - 공격 유형', 1, NOW(6), NOW(6));

-- id=23 보안 / 암호화 방식
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '보안' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '**대칭키 암호화**와 **비대칭키(공개키) 암호화**의 차이점을 설명하고, 각각의 대표적인 알고리즘을 하나씩 쓰시오.', 'DESCRIPTIVE', NULL, '대칭키 암호화는 암호화와 복호화에 동일한 키를 사용하는 방식으로 처리 속도가 빠르지만 키 배포 문제가 있다. 대표 알고리즘은 AES이다. 비대칭키 암호화는 공개키로 암호화하고 개인키로 복호화하는 방식으로 키 배포 문제를 해결하지만 처리 속도가 느리다. 대표 알고리즘은 RSA이다.', '["대칭키","동일한 키","AES","비대칭키","공개키","개인키","RSA","키 배포"]', '대칭키는 송수신자가 같은 키를 공유해야 하므로 키를 안전하게 전달하는 것이 과제이다. AES, DES, 3DES, SEED 등이 있다. 비대칭키는 공개키는 누구나 알 수 있고 개인키는 소유자만 보관한다. RSA, ECC, DSA 등이 있다. 실제로는 대칭키로 데이터를 암호화하고, 비대칭키로 대칭키를 전달하는 **하이브리드 방식**을 많이 사용한다.', '대칭키: 같은 키(AES), 비대칭키: 공개키+개인키(RSA)', '암호화 방식', 3, NOW(6), NOW(6));

-- id=24 보안 / 보안 공격과 대응
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '보안' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '**SQL 인젝션(SQL Injection)**의 공격 원리를 설명하고, 이를 방어하기 위한 대응 방안을 3가지 이상 서술하시오.', 'DESCRIPTIVE', NULL, 'SQL 인젝션은 웹 애플리케이션에서 사용자 입력값을 적절히 검증하지 않아, 공격자가 입력 필드에 악의적인 SQL 구문을 삽입하여 데이터베이스를 비정상적으로 조작하는 공격이다. 대응 방안으로는 첫째, 매개변수화된 쿼리(Prepared Statement)를 사용하여 사용자 입력을 SQL 구문과 분리한다. 둘째, 입력값 검증(Validation)을 수행하여 특수문자나 SQL 예약어를 필터링한다. 셋째, 웹 방화벽(WAF)을 도입하여 비정상적인 SQL 패턴을 탐지·차단한다. 넷째, 최소 권한 원칙을 적용하여 데이터베이스 접근 권한을 제한한다.', '["SQL 구문 삽입","입력값 검증 미흡","Prepared Statement","입력값 검증","WAF","최소 권한"]', 'SQL 인젝션은 OWASP Top 10에 포함되는 대표적 웹 취약점이다. 예를 들어 로그인 폼에서 `\' OR 1=1 --`을 입력하면 WHERE 조건이 항상 참이 되어 인증을 우회할 수 있다. **Prepared Statement**는 SQL 구문 구조를 미리 컴파일한 뒤 매개변수만 바인딩하므로 삽입된 SQL이 구문으로 해석되지 않는다. ORM 프레임워크 사용, 에러 메시지 노출 방지, 데이터베이스 계정의 최소 권한 부여도 중요한 방어 수단이다.', '입력값에 악성 SQL 삽입 → Prepared Statement 등으로 방어', '보안 공격과 대응', 5, NOW(6), NOW(6));

-- id=25 신기술 동향 / 클라우드 컴퓨팅 서비스 모델
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '신기술 동향' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '클라우드 컴퓨팅의 서비스 모델 중, 서버, 스토리지, 네트워크 등 인프라 자원을 가상화하여 제공하는 서비스 모델의 약어를 쓰시오.', 'SHORT_ANSWER', NULL, 'IaaS', '["IaaS","Infrastructure as a Service"]', '**IaaS(Infrastructure as a Service)**는 서버, 스토리지, 네트워크 등 IT 인프라를 가상화하여 제공하는 서비스이다. 사용자는 OS, 미들웨어, 애플리케이션을 직접 관리한다. AWS EC2, Azure VM 등이 대표적이다. 참고로 PaaS는 플랫폼, SaaS는 소프트웨어를 서비스 형태로 제공한다.', '인프라 가상화 제공 = IaaS', '클라우드 컴퓨팅 서비스 모델', 1, NOW(6), NOW(6));

-- id=26 신기술 동향 / 애자일 방법론
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '신기술 동향' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '애자일(Agile) 개발 방법론 중 **스크럼(Scrum)**의 주요 구성 요소인 스프린트(Sprint), 데일리 스크럼(Daily Scrum), 백로그(Backlog)의 개념을 각각 설명하시오.', 'DESCRIPTIVE', NULL, '스프린트는 2~4주 단위의 반복 개발 주기로, 이 기간 동안 계획된 기능을 구현하고 동작하는 소프트웨어를 산출한다. 데일리 스크럼은 매일 15분 이내로 진행되는 짧은 회의로, 팀원 각자가 어제 한 일, 오늘 할 일, 장애 요소를 공유한다. 백로그는 개발해야 할 기능 목록으로, 제품 백로그(전체 요구사항)와 스프린트 백로그(해당 스프린트에서 수행할 작업)로 나뉜다.', '["스프린트","2~4주 반복 주기","데일리 스크럼","15분 회의","백로그","기능 목록","제품 백로그"]', '스크럼은 Ken Schwaber와 Jeff Sutherland가 고안한 대표적 애자일 프레임워크이다. **스프린트**는 고정된 기간의 타임박스로 계획(Sprint Planning), 실행, 검토(Sprint Review), 회고(Sprint Retrospective) 과정을 거친다. **제품 소유자(Product Owner)**가 제품 백로그의 우선순위를 관리하고, **스크럼 마스터(Scrum Master)**가 프로세스를 촉진한다.', '스프린트=반복주기, 데일리스크럼=매일회의, 백로그=기능목록', '애자일 방법론', 3, NOW(6), NOW(6));

-- id=27 신기술 동향 / 컨테이너와 마이크로서비스
INSERT INTO question (subject_id, content, question_type, correct_option, answer, keywords, explanation, summary, topic, difficulty, created_at, updated_at) VALUES
    ((SELECT id FROM subject WHERE name = '신기술 동향' AND parent_id = (SELECT s.id FROM (SELECT id FROM subject WHERE name = '정보처리기사 실기' AND parent_id IS NULL) s)), '**컨테이너(Container)** 기술과 **마이크로서비스 아키텍처(MSA)**의 개념을 각각 설명하고, 두 기술이 결합되어 사용되는 이유를 서술하시오.', 'DESCRIPTIVE', NULL, '컨테이너는 애플리케이션과 그 실행 환경(라이브러리, 설정 등)을 하나의 패키지로 격리하여 어디서든 동일하게 실행할 수 있게 하는 경량 가상화 기술이다. 마이크로서비스 아키텍처는 하나의 애플리케이션을 작고 독립적인 서비스 단위로 분리하여 각 서비스가 독립적으로 배포·확장될 수 있도록 하는 설계 방식이다. 두 기술이 결합되는 이유는 컨테이너가 각 마이크로서비스를 독립적으로 패키징·배포·확장하기에 최적의 실행 단위를 제공하고, 쿠버네티스 같은 오케스트레이션 도구로 대규모 컨테이너를 효율적으로 관리할 수 있기 때문이다.', '["컨테이너","경량 가상화","격리","마이크로서비스","독립 배포","독립 확장","Docker","쿠버네티스"]', '컨테이너의 대표 기술은 **Docker**이며, 기존 VM 대비 OS 커널을 공유하므로 가볍고 빠르다. MSA는 모놀리식 아키텍처의 단점(배포 단위가 크고 한 부분의 수정이 전체에 영향)을 해결한다. 각 마이크로서비스를 독립된 컨테이너로 실행하면 서비스별 독립 배포, 기술 스택 다양성, 장애 격리가 가능해진다. **쿠버네티스(Kubernetes)**는 컨테이너의 자동 배포, 스케일링, 관리를 위한 오케스트레이션 플랫폼이다.', '컨테이너=경량 격리 실행, MSA=독립 서비스 분리, 결합=최적 배포 단위', '컨테이너와 마이크로서비스', 5, NOW(6), NOW(6));
