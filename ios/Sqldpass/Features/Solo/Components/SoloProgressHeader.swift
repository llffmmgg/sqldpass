import SwiftUI

/// 단일 채점 풀이 상단 헤더 — 종료/진행도/북마크/신고 + 진행 바.
struct SoloProgressHeader: View {
    let solvedCount: Int
    let totalCount: Int
    let correctCount: Int
    let isBookmarked: Bool
    let onClose: () -> Void
    let onToggleBookmark: () -> Void
    let onReport: () -> Void

    private var displayCurrent: Int { min(solvedCount + 1, totalCount) }
    private var progress: Double {
        guard totalCount > 0 else { return 0 }
        return Double(solvedCount) / Double(totalCount)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: Spacing.sm) {
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color.appTextPrimary)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("풀이 종료")

                VStack(alignment: .leading, spacing: 2) {
                    Text("\(displayCurrent) / \(totalCount)")
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                    Text("정답 \(correctCount) / \(solvedCount)")
                        .font(AppType.caption)
                        .foregroundStyle(Color.appTextMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: onToggleBookmark) {
                    Image(systemName: isBookmarked ? "star.fill" : "star")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(isBookmarked ? Color.brandPrimary : Color.appTextMuted)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel(isBookmarked ? "즐겨찾기 해제" : "즐겨찾기")

                Menu {
                    Button(role: .destructive, action: onReport) {
                        Label("이 문제 신고", systemImage: "exclamationmark.bubble")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color.appTextMuted)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("메뉴")
            }
            .padding(.horizontal, Spacing.sm)

            // 진행 바
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color.appElevated)
                    Rectangle()
                        .fill(Color.brandPrimary)
                        .frame(width: geo.size.width * progress)
                        .animation(.easeOut(duration: 0.2), value: progress)
                }
            }
            .frame(height: 3)
        }
        .background(Color.appSurface)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }
}
