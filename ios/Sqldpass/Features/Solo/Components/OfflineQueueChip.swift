import SwiftUI

/// 오프라인 답안 큐 미동기화 카운트 표시 — count > 0 일 때만 노출.
///
/// Android `OfflineQueueChip.kt` 와 동치. warning 톤 (실패가 아닌 "보관 중" 상태).
struct OfflineQueueChip: View {
    let count: Int

    var body: some View {
        if count > 0 {
            HStack(spacing: Spacing.xs) {
                Image(systemName: "cloud.slash")
                    .font(.system(size: 12, weight: .semibold))
                Text("오프라인 — \(count)개 보관 중")
                    .font(AppType.caption.weight(.semibold))
            }
            .foregroundStyle(Color.semanticWarning)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, Spacing.xs)
            .background(Color.semanticWarning.opacity(0.12))
            .clipShape(Capsule())
        }
    }
}
