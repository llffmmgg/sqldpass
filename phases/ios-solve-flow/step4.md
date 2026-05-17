# Step 4 — SolveResultView (채점 결과 화면)

## Background

풀이 제출 후 사용자가 점수와 정/오답 분석을 보는 화면. Step 3 에서 `SolveView` 가 `submittedResult: Solve` 를 받아 Step 5 에서 본 화면을 푸시한다.

표시 항목:
1. **총점 헤드라인** — 점수 큰 숫자, 정답 N / 총 M
2. **과목별 정답률** — 과목명 + 정답/총수 + 진행률 바
3. **문항 리스트** — 문항 번호 + 정/오답 아이콘 + 사용자 답 / 정답 + 해설 펼치기

## Workdir

```bash
ios/
```

## Dependencies

- Step 1~3 산출물 그대로 사용
- `Models/Solve.swift` — Solve, SolveAnswer
- `Models/MockExamDetail.swift` — MockExamQuestionItem (subjectName 매핑용)
- `Services/QuestionService` — 해설 fetch (옵션)

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Solve/SolveResultView.swift` | 신규 — 메인 결과 화면 |
| `ios/Sqldpass/Features/Solve/Components/ScoreHeadline.swift` | 신규 — 점수 큰 표시 |
| `ios/Sqldpass/Features/Solve/Components/SubjectBreakdownCard.swift` | 신규 — 과목별 정답률 |
| `ios/Sqldpass/Features/Solve/Components/AnswerReviewRow.swift` | 신규 — 문항별 정/오답 + 해설 |

## Implementation

### `SolveResultView.swift`

```swift
import SwiftUI

struct SolveResultView: View {
    let result: Solve
    let questions: [MockExamQuestionItem]
    let onDone: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                ScoreHeadline(
                    score: result.score,
                    correctCount: result.correctCount,
                    totalCount: result.totalCount,
                    milestoneReached: result.milestoneReached,
                    currentStreak: result.currentStreak
                )

                if !subjectBreakdown.isEmpty {
                    VStack(alignment: .leading, spacing: Spacing.sm) {
                        SectionHeader(title: "과목별 정답률")
                        ForEach(subjectBreakdown, id: \.name) { item in
                            SubjectBreakdownCard(
                                subjectName: item.name,
                                correct: item.correct,
                                total: item.total
                            )
                        }
                    }
                }

                VStack(alignment: .leading, spacing: Spacing.sm) {
                    SectionHeader(title: "문항별 검토")
                    ForEach(reviewItems, id: \.questionId) { item in
                        AnswerReviewRow(item: item)
                    }
                }
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle("채점 결과")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: Spacing.sm) {
                Button {
                    onDone()
                } label: {
                    Text("목록으로")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
            }
            .padding(.horizontal, Spacing.base)
            .padding(.bottom, Spacing.sm)
            .background(Color.appPage)
        }
    }

    // MARK: Derived

    private struct SubjectStat {
        let name: String
        var correct: Int
        var total: Int
    }

    private var subjectBreakdown: [SubjectStat] {
        // questionId → subjectName 매핑
        let subjectByQuestion: [Int64: String] = Dictionary(
            uniqueKeysWithValues: questions.map { ($0.id, $0.subjectName) }
        )
        var grouped: [String: SubjectStat] = [:]
        for answer in result.answers {
            let subject = subjectByQuestion[answer.questionId] ?? "기타"
            var stat = grouped[subject] ?? SubjectStat(name: subject, correct: 0, total: 0)
            stat.total += 1
            if answer.correct { stat.correct += 1 }
            grouped[subject] = stat
        }
        return grouped.values.sorted { $0.name < $1.name }
    }

    private var reviewItems: [AnswerReviewRow.Item] {
        let questionByid: [Int64: MockExamQuestionItem] = Dictionary(
            uniqueKeysWithValues: questions.map { ($0.id, $0) }
        )
        return result.answers.enumerated().map { idx, ans in
            let q = questionByid[ans.questionId]
            return AnswerReviewRow.Item(
                questionId: ans.questionId,
                displayOrder: q?.displayOrder ?? (idx + 1),
                content: q?.content ?? "(문제 정보 누락)",
                chosenAnswer: ans.chosenAnswer,
                isCorrect: ans.correct
            )
        }
    }
}

private struct SectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(AppType.bodyEmph)
            .foregroundStyle(Color.appTextPrimary)
    }
}
```

### `Components/ScoreHeadline.swift`

```swift
import SwiftUI

struct ScoreHeadline: View {
    let score: Int
    let correctCount: Int
    let totalCount: Int
    let milestoneReached: Int?
    let currentStreak: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(alignment: .firstTextBaseline, spacing: Spacing.xs) {
                Text("\(score)")
                    .font(AppType.display.weight(.bold))
                    .foregroundStyle(scoreColor)
                Text("점")
                    .font(AppType.title)
                    .foregroundStyle(Color.appTextMuted)
                Spacer()
            }
            Text("\(correctCount) / \(totalCount) 정답")
                .font(AppType.body)
                .foregroundStyle(Color.appTextMuted)
            if let milestone = milestoneReached {
                Label("\(milestone)일 연속 풀이 달성!", systemImage: "flame.fill")
                    .font(AppType.footnote.weight(.semibold))
                    .foregroundStyle(Color.semanticWarning)
            } else if let streak = currentStreak, streak > 0 {
                Label("연속 \(streak)일", systemImage: "flame")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
        }
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var scoreColor: Color {
        if score >= 80 { return Color.brandPrimary }
        if score >= 60 { return Color.semanticInfo }
        return Color.semanticDanger
    }
}
```

### `Components/SubjectBreakdownCard.swift`

```swift
import SwiftUI

struct SubjectBreakdownCard: View {
    let subjectName: String
    let correct: Int
    let total: Int

    private var rate: Double {
        guard total > 0 else { return 0 }
        return Double(correct) / Double(total)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text(subjectName)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Spacer()
                Text("\(correct) / \(total)")
                    .font(AppType.monoNumeric.weight(.semibold))
                    .foregroundStyle(rateColor)
            }
            ProgressView(value: rate)
                .tint(rateColor)
        }
        .padding(Spacing.md)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }

    private var rateColor: Color {
        if rate >= 0.8 { return Color.brandPrimary }
        if rate >= 0.6 { return Color.semanticInfo }
        return Color.semanticDanger
    }
}
```

### `Components/AnswerReviewRow.swift`

```swift
import SwiftUI

struct AnswerReviewRow: View {
    struct Item {
        let questionId: Int64
        let displayOrder: Int
        let content: String
        let chosenAnswer: String?
        let isCorrect: Bool
    }

    let item: Item

    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Button {
                isExpanded.toggle()
            } label: {
                HStack(alignment: .top, spacing: Spacing.sm) {
                    Image(systemName: item.isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                        .foregroundStyle(item.isCorrect ? Color.semanticSuccess : Color.semanticDanger)
                        .padding(.top, 2)
                    VStack(alignment: .leading, spacing: Spacing.xxs) {
                        Text("문제 \(item.displayOrder)")
                            .font(AppType.bodyEmph)
                            .foregroundStyle(Color.appTextPrimary)
                        Text("내 답: \(item.chosenAnswer ?? "미응답")")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.appTextMuted)
                    }
                    Spacer()
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.footnote)
                        .foregroundStyle(Color.appTextSubtle)
                }
            }
            .buttonStyle(.plain)

            if isExpanded {
                Text(item.content)
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
                    .padding(.top, Spacing.xs)
                Text("자세한 해설은 곧 지원 예정입니다.")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
            }
        }
        .padding(Spacing.md)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }
}
```

## Validation

### 빌드 검증 (필수)

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

### 스크린샷 검증 (선택, Step 5 에서 통합)

본 step 단독으로는 진입점이 없어 SwiftUI Preview 만으로 시각 확인. 빌드 통과가 필수.

## 금지사항

- 해설 본문 fetch (별도 API `/api/questions/{id}/explanation` 등) 신설 금지. 이유: 현재 `Question` DTO 에 explanation 필드가 없고 백엔드 변경이 필요한데 본 phase 는 iOS 단독 작업. 향후 별도 phase 에서 백엔드 + iOS 같이 진행.
- 차트 라이브러리 또는 Swift Charts 도입 금지. 이유: 본 화면은 진행률 바면 충분. Insights 화면에서 Charts 도입 예정 (별도 phase).
- `SolveResultView` 안에서 추가 API 호출 금지. 이유: 결과 객체는 이미 `Solve` 로 받아오고, 추가 fetch 는 race condition 위험. 부족한 메타데이터(과목명 등)는 호출 측이 전달.
- 다시 풀기 액션 추가 금지. 이유: 본 phase 는 1 회 풀이 사이클만. "다시 풀기" 는 별도 작업 — wrongAnswer retry 흐름과 통합 설계 필요.
