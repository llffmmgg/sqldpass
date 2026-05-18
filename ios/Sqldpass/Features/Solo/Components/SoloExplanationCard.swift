import SwiftUI

/// 정답 공개 시 노출 — 정답/오답 배너 + (서술/단답일 때) 모범답안 + 키워드 + 해설 카드.
struct SoloExplanationCard: View {
    let detail: QuestionDetail
    let isCorrect: Bool

    private var bannerColor: Color { isCorrect ? .semanticSuccess : .semanticDanger }
    private var type: String { detail.questionType.uppercased() }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            ResultBanner(detail: detail, isCorrect: isCorrect, color: bannerColor)

            if type.contains("SHORT") || type.contains("DESCRIPTIVE") || type.contains("TEXT") {
                ModelAnswerCard(detail: detail)
            }

            if let explanation = detail.explanation, !explanation.isEmpty {
                ExplanationCard(text: explanation)
            }
        }
    }
}

private struct ResultBanner: View {
    let detail: QuestionDetail
    let isCorrect: Bool
    let color: Color

    private var message: String {
        let type = detail.questionType.uppercased()
        if isCorrect { return "정답입니다!" }
        if type.contains("MCQ") || type.contains("MULTIPLE") {
            return "오답 — 정답은 \(detail.correctOption.map(String.init) ?? "-")번입니다."
        }
        return "오답 — 모범답안: \(detail.answer ?? "(없음)")"
    }

    var body: some View {
        HStack(spacing: Spacing.sm) {
            Image(systemName: isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(color)
            Text(message)
                .font(AppType.bodyEmph)
                .foregroundStyle(color)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.md)
        .background(color.opacity(0.10))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(color, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

private struct ModelAnswerCard: View {
    let detail: QuestionDetail
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("모범답안")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            Text(detail.answer?.isEmpty == false ? detail.answer! : "-")
                .font(.system(.body, design: .monospaced))
                .foregroundStyle(Color.appTextPrimary)
            if !detail.keywords.isEmpty {
                Text(detail.questionType.uppercased().contains("SHORT") ? "허용 표기" : "채점 키워드")
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextMuted)
                    .padding(.top, Spacing.xs)
                KeywordsFlow(keywords: detail.keywords)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Spacing.base)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

private struct KeywordsFlow: View {
    let keywords: [String]
    var body: some View {
        // iOS 16+ — `LazyVGrid` 로 단순 wrap. row 폭 측정 없이 깔끔.
        let columns = [GridItem(.adaptive(minimum: 80), spacing: Spacing.xs)]
        LazyVGrid(columns: columns, alignment: .leading, spacing: Spacing.xs) {
            ForEach(Array(keywords.enumerated()), id: \.offset) { _, kw in
                Text(kw)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.semanticSuccess)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xs)
                    .background(Color.semanticSuccess.opacity(0.10))
                    .clipShape(Capsule())
            }
        }
    }
}

private struct ExplanationCard: View {
    let text: String
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("해설")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.brandPrimary)
            QuestionContentView(text: text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Spacing.base)
        .background(Color.brandPrimary.opacity(0.06))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.brandPrimary.opacity(0.30), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}
