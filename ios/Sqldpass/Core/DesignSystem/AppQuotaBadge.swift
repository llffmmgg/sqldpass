import SwiftUI

/// 무료 회원 일일 한도 진행 헤더 배지.
///
/// 예시: "오늘 18 / 30 문제", "오늘 0 / 1 모의고사".
///
/// 사용 규칙:
///  - 탭 진입 헤더에 한 번만 마운트. 내부에서 `QuotaService.fetch()` 호출.
///  - 활성 구독자(`limit == nil`)는 EmptyView — 표시하지 않는다.
///  - 기출복원 화면에는 마운트 금지 — 서버가 402 던지지 않아 항상 `0/limit` 가 떠 혼란 유발.
///  - 자체 카운팅 금지 — 화면 재진입 또는 부모 `refreshable` 흐름에서 다시 fetch.
enum AppQuotaKind {
    case question  // /api/questions
    case mock      // /api/mock-exams/{id}

    var unitLabel: String {
        switch self {
        case .question: return "문제"
        case .mock:     return "모의고사"
        }
    }
}

struct AppQuotaBadge: View {
    let kind: AppQuotaKind
    @State private var quota: Quota?
    @State private var didLoad: Bool = false

    var body: some View {
        Group {
            if let quota,
               let (used, limit) = pair(from: quota), limit > 0 {
                pill(used: used, limit: limit)
            } else {
                // 활성 구독자(limit nil) 또는 로딩 전 — 공간 차지 X.
                EmptyView()
            }
        }
        .task {
            // 마운트 시 1회 fetch. 실패 시 조용히 숨김 — 학습 흐름에 모달/배너를 띄우지 않는다.
            guard !didLoad else { return }
            didLoad = true
            quota = try? await QuotaService.fetch()
        }
    }

    private func pill(used: Int, limit: Int) -> some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: "calendar")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextSubtle)
            Text("오늘")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
            Text("\(used)")
                .font(AppType.caption.weight(.semibold).monospacedDigit())
                .foregroundStyle(used >= limit ? Color.semanticWarning : Color.appTextPrimary)
            Text("/ \(limit) \(kind.unitLabel)")
                .font(AppType.caption.monospacedDigit())
                .foregroundStyle(Color.appTextMuted)
        }
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, Spacing.xxs)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.full)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.full))
    }

    /// kind 별 (used, limit?) 추출.
    private func pair(from q: Quota) -> (Int, Int)? {
        switch kind {
        case .question:
            guard let limit = q.questionLimit else { return nil }
            return (q.questionUsed, limit)
        case .mock:
            guard let limit = q.mockLimit else { return nil }
            return (q.mockUsed, limit)
        }
    }
}

#Preview {
    VStack(spacing: Spacing.lg) {
        AppQuotaBadge(kind: .question)
        AppQuotaBadge(kind: .mock)
    }
    .padding(Spacing.lg)
    .background(Color.appPage)
}
