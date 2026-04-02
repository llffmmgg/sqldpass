-- 로컬 환경 시드 데이터 (local 프로필에서만 실행)
-- 임시 회원
INSERT IGNORE INTO member (id, provider, provider_id, nickname, email, profile_image, created_at, updated_at)
VALUES (1, 'local', 'local-1', '테스트유저', 'test@test.com', NULL, NOW(6), NOW(6));

-- === 과목 3: 데이터 모델링의 이해 ===

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (1, 3,
'엔터티(Entity)에 대한 설명으로 가장 적절하지 않은 것은?\n\n① 업무에 필요하고 유용한 정보를 저장하고 관리하기 위한 집합\n② 반드시 해당 업무에서 필요하며 관리하고자 하는 정보여야 한다\n③ 엔터티는 반드시 하나 이상의 식별자(Unique Identifier)가 있어야 한다\n④ 엔터티는 하나의 인스턴스만 존재해도 성립한다',
4,
'엔터티는 두 개 이상의 인스턴스 집합이어야 합니다. 하나의 인스턴스만으로는 엔터티로 성립할 수 없습니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (2, 3,
'다음 중 속성(Attribute)의 특성으로 가장 적절하지 않은 것은?\n\n① 엔터티를 설명하는 가장 작은 데이터 단위이다\n② 하나의 속성은 하나의 값만 가져야 한다\n③ 주식별자에 함수적으로 종속되어야 한다\n④ 하나의 속성은 여러 엔터티에 동시에 포함될 수 있다',
4,
'하나의 속성은 하나의 엔터티에만 속해야 합니다. 여러 엔터티에서 같은 이름의 속성이 있을 수 있지만, 같은 속성이 동시에 여러 엔터티에 포함되는 것은 아닙니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (3, 3,
'관계(Relationship)에 대한 설명으로 가장 적절한 것은?\n\n① 관계는 항상 양방향으로 존재해야 한다\n② 관계의 카디널리티는 1:1, 1:M, M:N이 있다\n③ 관계는 반드시 물리적으로 외래키로 구현해야 한다\n④ 재귀 관계(Recursive Relationship)는 데이터 모델에서 허용되지 않는다',
2,
'관계의 카디널리티(Cardinality)는 1:1, 1:M(일대다), M:N(다대다)의 세 가지 유형이 있습니다.',
NOW(6), NOW(6));

-- === 과목 4: 데이터 모델과 SQL ===

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (4, 4,
'정규화에 대한 설명으로 가장 적절하지 않은 것은?\n\n① 데이터의 중복을 제거하고 이상현상을 방지한다\n② 제1정규형은 모든 속성이 원자값을 가져야 한다\n③ 제2정규형은 부분 함수적 종속을 제거한 것이다\n④ 반정규화는 정규화를 수행하지 않는 것을 말한다',
4,
'반정규화(Denormalization)는 정규화된 엔터티를 성능 향상 등의 이유로 의도적으로 중복, 통합, 분리하는 것이지, 정규화를 하지 않는 것이 아닙니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (5, 4,
'다음 중 NULL에 대한 설명으로 가장 적절한 것은?\n\n① NULL은 0과 같은 의미이다\n② NULL과 어떤 값을 비교하면 항상 FALSE이다\n③ NULL끼리의 비교 결과는 TRUE이다\n④ NULL과의 연산 결과는 NULL이다',
4,
'NULL과의 모든 연산(산술, 비교) 결과는 NULL입니다. NULL은 0이나 공백과 다르며, NULL끼리 비교해도 UNKNOWN입니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (6, 4,
'슈퍼타입/서브타입 모델의 변환 방법이 아닌 것은?\n\n① 개별 타입 변환 (각각의 테이블로)\n② 슈퍼+서브 타입 변환 (하나의 테이블로)\n③ 슈퍼 타입만 변환 (슈퍼 타입 테이블만 생성)\n④ 서브 타입끼리 조인 변환 (서브 타입끼리 관계 생성)',
4,
'슈퍼타입/서브타입의 물리 모델 변환 방식은 개별 타입(1:1), 슈퍼+서브 통합(All-in-One), 서브 타입별 개별 테이블(Plus) 세 가지입니다.',
NOW(6), NOW(6));

-- === 과목 5: SQL 기본 ===

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (7, 5,
'다음 SQL의 실행 결과로 올바른 것은?\n\nSELECT COUNT(*), COUNT(COL1) FROM T1;\n\n(T1 테이블: COL1 값이 1, NULL, 3, NULL 인 4건)\n\n① 4, 4\n② 4, 2\n③ 2, 2\n④ 2, 4',
2,
'COUNT(*)는 NULL을 포함한 전체 행의 수(4)를 반환하고, COUNT(COL1)은 COL1이 NULL이 아닌 행의 수(2)를 반환합니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (8, 5,
'WHERE 절에서 NULL 값을 검색하기 위한 올바른 방법은?\n\n① WHERE COL = NULL\n② WHERE COL IS NULL\n③ WHERE COL == NULL\n④ WHERE COL LIKE NULL',
2,
'NULL 값은 IS NULL 또는 IS NOT NULL 연산자로만 비교할 수 있습니다. = 연산자로는 NULL을 비교할 수 없습니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (9, 5,
'DELETE, TRUNCATE, DROP에 대한 설명으로 가장 적절하지 않은 것은?\n\n① DELETE는 DML이며 ROLLBACK이 가능하다\n② TRUNCATE는 DDL이며 ROLLBACK이 불가능하다\n③ DROP은 테이블의 구조까지 삭제한다\n④ TRUNCATE는 로그를 남기므로 DELETE보다 느리다',
4,
'TRUNCATE는 로그를 남기지 않고 데이터를 삭제하므로 DELETE보다 빠릅니다. DELETE는 행 단위로 로그를 기록합니다.',
NOW(6), NOW(6));

-- === 과목 6: SQL 활용 ===

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (10, 6,
'다음 중 서브쿼리의 종류와 설명이 올바르게 연결된 것은?\n\n① 스칼라 서브쿼리 - WHERE절에서 사용\n② 인라인 뷰 - SELECT절에서 사용\n③ 스칼라 서브쿼리 - 단일 값을 반환하며 SELECT절에서 사용 가능\n④ 인라인 뷰 - 단일 값만 반환 가능',
3,
'스칼라 서브쿼리는 하나의 행에서 하나의 컬럼 값만 반환하는 서브쿼리로, SELECT절에서 사용 가능합니다. 인라인 뷰는 FROM절에서 사용하는 서브쿼리입니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (11, 6,
'윈도우 함수(Window Function)에 대한 설명으로 가장 적절하지 않은 것은?\n\n① RANK()는 동일 값에 같은 순위를 부여하고 다음 순위를 건너뛴다\n② DENSE_RANK()는 동일 값에 같은 순위를 부여하고 다음 순위를 건너뛰지 않는다\n③ ROW_NUMBER()는 동일 값이라도 고유한 순위를 부여한다\n④ 윈도우 함수는 WHERE절에서 사용할 수 있다',
4,
'윈도우 함수는 SELECT절과 ORDER BY절에서만 사용할 수 있으며, WHERE절이나 GROUP BY절에서는 사용할 수 없습니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (12, 6,
'다음 JOIN에 대한 설명으로 가장 적절한 것은?\n\n① INNER JOIN은 한쪽 테이블에만 데이터가 있어도 결과에 포함된다\n② LEFT OUTER JOIN은 오른쪽 테이블의 모든 행을 포함한다\n③ CROSS JOIN은 두 테이블의 모든 조합(카테시안 곱)을 반환한다\n④ NATURAL JOIN은 ON 조건을 반드시 명시해야 한다',
3,
'CROSS JOIN은 두 테이블의 카테시안 곱을 반환합니다. INNER JOIN은 양쪽 모두 매칭되어야 하고, LEFT OUTER JOIN은 왼쪽 테이블의 모든 행을 포함합니다.',
NOW(6), NOW(6));

-- === 과목 7: 관리 구문 ===

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (13, 7,
'DCL(Data Control Language)에 해당하는 것은?\n\n① CREATE\n② INSERT\n③ GRANT\n④ ALTER',
3,
'GRANT와 REVOKE가 DCL에 해당합니다. CREATE, ALTER, DROP은 DDL이고, INSERT, UPDATE, DELETE는 DML입니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (14, 7,
'트랜잭션의 특성(ACID)에 대한 설명으로 가장 적절하지 않은 것은?\n\n① 원자성(Atomicity) - 트랜잭션은 모두 실행되거나 모두 취소되어야 한다\n② 일관성(Consistency) - 트랜잭션 실행 후에도 데이터 무결성이 유지되어야 한다\n③ 격리성(Isolation) - 동시에 실행되는 트랜잭션은 서로 영향을 미치지 않아야 한다\n④ 지속성(Durability) - 트랜잭션은 일정 시간이 지나면 자동으로 COMMIT된다',
4,
'지속성(Durability)은 트랜잭션이 성공적으로 COMMIT되면 그 결과가 영구적으로 반영되어야 한다는 의미입니다. 자동 COMMIT과는 관련이 없습니다.',
NOW(6), NOW(6));

INSERT IGNORE INTO question (id, subject_id, content, correct_option, explanation, created_at, updated_at)
VALUES (15, 7,
'SAVEPOINT에 대한 설명으로 가장 적절한 것은?\n\n① SAVEPOINT는 COMMIT 이후에도 해당 지점으로 ROLLBACK할 수 있다\n② SAVEPOINT를 사용하면 트랜잭션의 일부만 ROLLBACK할 수 있다\n③ SAVEPOINT는 DDL 문에서만 사용할 수 있다\n④ 하나의 트랜잭션에 SAVEPOINT는 하나만 설정할 수 있다',
2,
'SAVEPOINT를 사용하면 트랜잭션 내 특정 지점까지만 ROLLBACK할 수 있습니다. COMMIT 이후에는 ROLLBACK이 불가하며, 여러 개의 SAVEPOINT를 설정할 수 있습니다.',
NOW(6), NOW(6));
