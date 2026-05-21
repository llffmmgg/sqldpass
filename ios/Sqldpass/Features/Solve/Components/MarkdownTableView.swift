import SwiftUI

/// Native GFM table renderer for question content.
struct MarkdownTableView: View {
    let rows: [[String]]

    private let minCellWidth: CGFloat = 92
    private let maxCellWidth: CGFloat = 220
    private let estimatedCharacterWidth: CGFloat = 7.5

    private var columnCount: Int {
        rows.map(\.count).max() ?? 0
    }

    private var normalizedRows: [[String]] {
        guard columnCount > 0 else { return [] }
        return rows.map { row in
            if row.count == columnCount {
                return row
            }
            if row.count > columnCount {
                return Array(row.prefix(columnCount))
            }
            return row + Array(repeating: "", count: columnCount - row.count)
        }
    }

    private var columnWidths: [CGFloat] {
        guard columnCount > 0 else { return [] }
        return (0..<columnCount).map { column in
            let longest = normalizedRows
                .map { $0[column].count }
                .max() ?? 0
            let contentWidth = CGFloat(max(longest, 6)) * estimatedCharacterWidth + (Spacing.md * 2)
            return min(max(contentWidth, minCellWidth), maxCellWidth)
        }
    }

    var body: some View {
        if normalizedRows.isEmpty {
            EmptyView()
        } else {
            ScrollView(.horizontal, showsIndicators: true) {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(normalizedRows.enumerated()), id: \.offset) { rowIndex, row in
                        rowView(rowIndex: rowIndex, row: row)
                    }
                }
                .fixedSize(horizontal: true, vertical: false)
            }
            .scrollIndicators(.visible, axes: .horizontal)
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            .background(Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.md)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
        }
    }

    private func rowView(rowIndex: Int, row: [String]) -> some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 0) {
                ForEach(Array(row.enumerated()), id: \.offset) { columnIndex, cell in
                    cellView(cell, rowIndex: rowIndex, columnIndex: columnIndex)
                }
            }
            .background(rowBackground(rowIndex))

            if rowIndex == 0 {
                Rectangle()
                    .fill(Color.appBorderStrong)
                    .frame(height: 1)
            } else if rowIndex < normalizedRows.count - 1 {
                Rectangle()
                    .fill(Color.appBorder)
                    .frame(height: 1)
            }
        }
    }

    private func cellView(_ cell: String, rowIndex: Int, columnIndex: Int) -> some View {
        Text(cell.isEmpty ? " " : cell)
            .font(rowIndex == 0
                  ? AppType.footnote.weight(.semibold)
                  : .system(.footnote, design: .monospaced))
            .foregroundStyle(Color.appTextPrimary)
            .multilineTextAlignment(.leading)
            .lineLimit(nil)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, rowIndex == 0 ? Spacing.sm : Spacing.sm + 2)
            .frame(width: columnWidths[columnIndex], alignment: .topLeading)
            .overlay(alignment: .trailing) {
                if columnIndex < columnCount - 1 {
                    Rectangle()
                        .fill(Color.appBorder)
                        .frame(width: 1)
                }
            }
    }

    private func rowBackground(_ rowIndex: Int) -> Color {
        if rowIndex == 0 {
            return Color.appElevated
        }
        return rowIndex.isMultiple(of: 2)
            ? Color.appSurface
            : Color.appElevated.opacity(0.35)
    }
}
