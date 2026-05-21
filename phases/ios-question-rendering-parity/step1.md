# Step 1 — EnsureCodeFences: 평문 자동 펜싱 + 옵션 마커 줄바꿈 정규화

## 배경

iOS 네이티브 SwiftUI 앱은 백엔드 마크다운 응답을 `Core/Content/EnsureCodeFences.swift` → `Core/Content/ParseQuestionContent.swift` 순서로 정규화하고, 그 결과 segments 를 `Features/Solve/Components/QuestionContentView.swift` 가 화면 분기 렌더한다.

웹(`frontend/src/components/QuestionContent.tsx:27-122`) 의 `ensureCodeFences()` 는 펜스 없이 저장된 SQL/C/Java/Python 라인을 휴리스틱으로 자동 펜싱하지만, iOS `EnsureCodeFences.normalize` 는 HTML(`<pre><code>`, `<code>`, `<table>`) → 마크다운 변환만 한다. 그래서 레거시 SQLD 문제(SQL 평문 저장) 는 웹은 코드블록, iOS 는 일반 문단으로 표시되어 줄바꿈/들여쓰기/모노가 모두 사라진다.

또한 사용자 보고로 "보기 1.ㅁㄴㅇ 2.ㅁㄴㅇ 줄바꿈 안 되는 케이스" 가 있다. 보기 분리(`QuestionParser`) 는 `①②③④` 만 인식하므로 `1./2./3./4.` 또는 4 마커가 한 줄에 다 붙은 보기는 분리 실패해 본문 영역에 한 덩어리로 표시된다. `QuestionParser` 자체는 웹/Android(`mobile/.../text/QuestionParser.kt`) 와 1:1 동치라 건드리지 않고, 정규화 단계에서 마커 앞에 `\n` 만 삽입해 최소한 줄바꿈은 살린다.

## 작업 디렉터리

```
ios/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `ios/Sqldpass/Core/Content/EnsureCodeFences.swift` | `normalize(_:)` 끝에 `autoFencePlainCode(_:)` + `normalizeOptionMarkers(_:)` 두 단계 추가. HTML 변환 3 단계는 그대로. |

## 구현 상세

### normalize 순서
```swift
static func normalize(_ input: String) -> String {
    var text = input
    text = replaceHtmlCodeBlocks(text)
    text = replaceHtmlInlineCode(text)
    text = replaceHtmlTables(text)
    text = autoFencePlainCode(text)          // 신규
    text = normalizeOptionMarkers(text)       // 신규
    return text
}
```

### autoFencePlainCode — 웹 `ensureCodeFences` 와 동치
웹 `frontend/src/components/QuestionContent.tsx:27-122` 의 알고리즘을 Swift 로 그대로 옮긴다.

규칙:
- 한글(`[가-힣]`) 포함 라인은 코드로 보지 않음 (false-positive 방지).
- 이미 ```...``` 펜스 안인 라인은 통과.
- `SQL_START`: `SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH|MERGE|TRUNCATE|GRANT|REVOKE|COMMIT|ROLLBACK|SAVEPOINT` 시작 (case-insensitive).
- `SQL_CONT`: `FROM|WHERE|AND|OR|GROUP BY|ORDER BY|HAVING|JOIN|...|RANGE|ROWS|TO|PUBLIC` (웹 정규식 그대로).
- `C_START`: `#include`, `int main`, `int X (=;([)`, `void X (`, `char/float/double/long/short/unsigned`, `struct X`, `typedef`, `return 숫자`, `printf(`, `scanf(`, `while(`, `for(`, `if(`, `do {`, `switch(`.
- `JAVA_START`: `public (static)? (class|void|int|String|boolean)`, `private X`, `protected X`, `class X {?`, `System.out.print`, `void X(`, `try {`, `catch(`.
- `PY_START`: `def X(`, `class X :|(`, `import X`, `from X import`, `print(`, `for X in `, `while ... :`, `if ... :`.
- 연속 라인: 빈 라인 만나면 끝, 한글 라인 만나면 끝, 들여쓰기(`^\s{2,}\S`) 또는 기호(`[{}();,]`, `//`, `/*`, `*/`, `else`, `return`) 또는 언어별 `*_START`/`SQL_CONT` 매치면 같은 블록.
- 1 줄 + 40 자 미만이면 펜싱 보류 (`for(;;)` 같은 짧은 토큰은 인라인 처리에 맡김).

Swift 구현 메모:
- `NSRegularExpression` 정규식 8 개를 `static let` 으로 캐시 (`hasKoreanRegex`, `sqlStartRegex`, `sqlContRegex`, `cStartRegex`, `javaStartRegex`, `pyStartRegex`, `contIndentedRegex`, `contSymbolsRegex`).
- 입력은 `components(separatedBy: "\n")` 으로 line array, 결과는 `joined(separator: "\n")`.
- 펜스 추적은 단순 토글(`inFence: Bool`) — 첫 ` ```` 만남 시 true, 다음 ` ```` 만남 시 false. 그 사이 라인은 출력 array 에 그대로 append.

### normalizeOptionMarkers — 보수적 줄바꿈 삽입
규칙 (false-positive 최소화):
- 펜스 블록 안은 건드리지 않음 (위 inFence 추적과 같은 함수에 묶거나 별도 패스).
- 한 라인 안에서 다음 3 케이스 중 하나 만족할 때만 트리거:
  1. `①` `②` `③` `④` **모두** 등장.
  2. `1.` `2.` `3.` `4.` **모두** 등장 (단어 경계 뒤). 정규식 `(?<![\d.])\d\.` 식으로 `12.` 등 오매치 제외.
  3. `1)` `2)` `3)` `4)` **모두** 등장.
- 트리거 시 각 마커 위치 직전(첫 마커 제외) 에 `\n` 삽입.
- 또한 별도로, 라인 시작이 마커가 아닌데 라인 중간에 `①` 단 하나라도 등장하면 그 앞에 `\n` 삽입 (본문 끝에 보기가 바로 이어 붙은 경우 대응 — false-positive 거의 없음, `①` 는 본문에 거의 안 등장).
- 마커 자체 변환(`1.` → `①`) 은 하지 않음 (`QuestionParser` 가 본 결과를 다시 받지만 본 phase 는 분리 로직 변경 없이 줄바꿈만 살림).

## 검증

Windows 셸에서는 iOS 빌드 불가. Swift 단위 테스트 타겟도 본 프로젝트에 없음. 본 step 의 회귀 안전성은 코드 리뷰 + Step 6 의 시뮬레이터 스크린샷으로 확인.

자가 검증용 mental check (구현 직후 head 안 시뮬레이션):
- 입력 `"SELECT *\nFROM t\nWHERE x = 1"` → 출력에 ` ```sql ... ``` ` 펜스 추가.
- 입력 `"문제: 다음 SELECT 문의 결과는?\n```sql\nSELECT 1\n```\n①첫번째②두번째③세번째④네번째"` → 펜스 내부 무변경, 마지막 줄에 `①` 앞 빼고 `②③④` 앞에 `\n` 삽입.
- 입력 `"한글 문장 SELECT * FROM x"` → 한글 라인이라 펜싱 안 함.
- 입력 `"1. 옳다 2. 그르다 3. 알 수 없다 4. 데이터 부족"` → 4 마커 다 있음 → 줄바꿈 삽입.
- 입력 `"~의 1. 원칙은 어떻게 되는가?"` → `1.` 만 있고 `2./3./4.` 없음 → 무변경.

## Acceptance Criteria

1. `EnsureCodeFences.swift` 의 `normalize` 가 위 2 단계를 추가로 통과.
2. 기존 HTML 변환(`<pre><code>` / `<code>` / `<table>` / HTML 엔티티) 동작 회귀 0.
3. Swift 컴파일 에러 0 (xcodebuild 검증은 Step 6 에서 사용자가 수행).
4. autoFencePlainCode 가 한글 라인 false-positive 를 만들지 않음.
5. normalizeOptionMarkers 는 위 3 트리거 외 본문을 건드리지 않음.

## 금지 사항

- `QuestionParser.swift` 의 마커 셋을 확장하지 마라. **이유**: 웹/Android `parseQuestion` 과 1:1 동치 유지 정책 + `1.` 본문 false-positive 위험.
- `ParseQuestionContent.swift` 의 `fencedRegex` 패턴을 변경하지 마라. **이유**: 펜스 정규식은 안정 동작 중이고 본 step 의 휴리스틱은 그 입력 단계에서만 작동.
- `autoFencePlainCode` 안에서 마크다운 헤딩(`#`), 리스트(`-`, `1.`) 같은 마크다운 구문을 코드로 오인하지 마라. **이유**: `1.` 만으로는 코드 시작 시그널이 아님(`*_START` 규칙에 없음).
- 펜스 안 라인을 휴리스틱이 다시 펜싱하지 마라. **이유**: 이중 펜스로 ``` ``` ``` ``` ``` 가 되어 파서가 실패.
- 색/디자인 토큰을 변경하지 마라. **이유**: 본 step 은 텍스트 정규화만 다룸.

## Status 규칙

- 성공: `completed` + summary "EnsureCodeFences.normalize 에 autoFencePlainCode + normalizeOptionMarkers 추가, 한글/펜스 인지, 4 마커 동시 등장만 트리거".
- 실패: 3회 재시도 후 `error`.
