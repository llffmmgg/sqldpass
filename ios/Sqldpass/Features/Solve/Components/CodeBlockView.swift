import SwiftUI

/// 코드블록 — 다크톤 surface + 가벼운 신택스 하이라이팅 + 가로 스크롤.
///
/// 라이트/다크 모드 무관 다크톤 고정 (웹 zinc-900 / oneDark 정책과 매칭).
enum CodeBlockSurface {
    case card
    case bare
}

struct CodeBlockView: View {
    let language: String?
    let code: String
    let surface: CodeBlockSurface

    init(language: String?, code: String, surface: CodeBlockSurface = .card) {
        self.language = language
        self.code = code
        self.surface = surface
    }

    @ViewBuilder
    var body: some View {
        switch surface {
        case .card:
            cardContent
        case .bare:
            bareContent
        }
    }

    private var cardContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            if hasLanguage {
                languageCaption
                    .padding(.horizontal, Spacing.md)
                    .padding(.vertical, Spacing.xs)
                    .background(Color.appCodeHeader)

                Rectangle()
                    .fill(Color.appCodeBorder)
                    .frame(height: 1)
            }

            highlightedCodeScroll(padding: Spacing.md)
                .background(Color.appCodeSurface)
        }
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appCodeBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }

    private var bareContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            if hasLanguage {
                languageCaption
                    .padding(.horizontal, Spacing.sm)
                    .padding(.top, Spacing.xs)
                    .padding(.bottom, 2)
            }

            highlightedCodeScroll(padding: Spacing.sm)
        }
        .background(Color.appCodeSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.sm))
    }

    @ViewBuilder
    private var languageCaption: some View {
        if let lang = language, !lang.isEmpty {
            Text(lang.uppercased())
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appCodeText.opacity(0.7))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var hasLanguage: Bool {
        guard let language else { return false }
        return !language.isEmpty
    }

    private func highlightedCodeScroll(padding: CGFloat) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            Text(SimpleSyntaxHighlighter.highlight(code, language: language))
                .lineLimit(nil)
                .fixedSize(horizontal: true, vertical: false)
                .padding(padding)
        }
    }
}
