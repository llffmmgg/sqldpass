import SwiftUI

/// 코드블록 — mono font + 가로 스크롤 + 언어 라벨 칩.
///
/// Android `CodeBlockCard` 와 동등한 시각 수준 (단색, 신택스 하이라이팅 없음).
/// 하이라이팅은 별도 phase 에서 검토.
struct CodeBlockView: View {
    let language: String?
    let code: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let lang = language, !lang.isEmpty {
                Text(lang.uppercased())
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextSubtle)
                    .padding(.horizontal, Spacing.md)
                    .padding(.vertical, Spacing.xs)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.appElevated)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                Text(code)
                    .font(.system(.footnote, design: .monospaced))
                    .foregroundStyle(Color.appTextPrimary)
                    .lineLimit(nil)
                    .fixedSize(horizontal: true, vertical: false)
                    .padding(Spacing.md)
            }
            .background(Color.appSurface)
        }
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }
}
