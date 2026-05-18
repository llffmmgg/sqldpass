import SwiftUI

/// GFM table 을 가로 스크롤 가능한 격자로 렌더.
///
/// SwiftUI native `AttributedString(markdown:)` 은 GFM table 을 지원하지 않으므로
/// 본 뷰가 직접 처리. Android 는 Markwon `TablePlugin` 이 알아서 처리.
///
/// 셀은 모두 left-align, 헤더는 elevated 배경 + semibold,
/// 본문 셀은 mono font (코드/숫자 정렬 가독성).
struct MarkdownTableView: View {
    let rows: [[String]]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            VStack(spacing: 0) {
                ForEach(Array(rows.enumerated()), id: \.offset) { idx, row in
                    HStack(spacing: 0) {
                        ForEach(Array(row.enumerated()), id: \.offset) { _, cell in
                            Text(cell)
                                .font(idx == 0
                                      ? AppType.footnote.weight(.semibold)
                                      : .system(.footnote, design: .monospaced))
                                .foregroundStyle(idx == 0
                                                 ? Color.appTextPrimary
                                                 : Color.appTextPrimary)
                                .padding(.horizontal, Spacing.md)
                                .padding(.vertical, Spacing.sm)
                                .frame(minWidth: 80, alignment: .leading)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(idx == 0 ? Color.appElevated : Color.appSurface)
                    if idx < rows.count - 1 {
                        Rectangle()
                            .fill(Color.appBorder)
                            .frame(height: 1)
                    }
                }
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: Radius.md)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
    }
}
