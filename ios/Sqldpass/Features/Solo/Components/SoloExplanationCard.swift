import SwiftUI

/// 정답 공개 시 노출 — `AppCard` + `AppBadge` 기반의 결과/해설 카드.
///
/// 좌측 액센트 바: 정답 시 success, 오답 시 danger.
/// 본문 구성: `AppBadge` (정답/오답) + 정답 표기 + (선택) 키워드 칩 스트립 + 해설 본문.
/// 본문 렌더링은 frozen `QuestionContentView` 를 그대로 사용한다.
struct SoloExplanationCard: View {
    let detail: QuestionDetail
    let isCorrect: Bool

    var body: some View {
        AppCard(surface: .card, accent: isCorrect ? .success : .danger) {
            VStack(alignment: .leading, spacing: Spacing.md) {
                HStack(spacing: Spacing.sm) {
                    AppBadge(label: isCorrect ? "정답" : "오답",
                             tone: isCorrect ? .success : .danger,
                             variant: .solid)
                    if let correctOption = detail.correctOption {
                        Text("정답 — \(correctOption)번")
                            .font(AppType.bodyEmph)
                            .foregroundStyle(Color.appTextPrimary)
                    } else if let answer = detail.answer, !answer.isEmpty {
                        Text("정답 — \(answer)")
                            .font(AppType.bodyEmph)
                            .foregroundStyle(Color.appTextPrimary)
                    }
                }

                if !detail.keywords.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: Spacing.xs) {
                            ForEach(detail.keywords, id: \.self) { kw in
                                AppBadge(label: kw, tone: .accent, variant: .soft)
                            }
                        }
                    }
                }

                if let explanation = detail.explanation, !explanation.isEmpty {
                    QuestionContentView(text: explanation)
                }
            }
        }
    }
}
