import SwiftUI

/// 무료 회원 일일 한도 도달 시 노출되는 페이월 시트.
///
/// 서버가 402 `quotaExceeded` 응답을 던질 때 호출 측 ViewModel 이
/// `QuotaPaywallInfo` 를 만들어 `.sheet(item:)` 으로 띄운다.
///
/// 카운팅·차단 로직은 본 뷰에 없다 — 서버 단일 진실 소스.
/// `Focus 7일권 보기` 가 기본 CTA 이지만 결제 흐름은 `PaywallView` 로 위임한다.
struct QuotaPaywallInfo: Identifiable, Equatable {
    let id = UUID()
    /// "DAILY_QUESTION_LIMIT" | "DAILY_MOCK_LIMIT"
    let code: String
    let used: Int
    let limit: Int
    /// KST naive ISO (예: "2026-05-22T00:00:00"). 표시 시 +09:00 가정.
    let resetAt: String

    var isMock: Bool { code == "DAILY_MOCK_LIMIT" }
}

struct QuotaPaywallView: View {
    let info: QuotaPaywallInfo
    let onClose: () -> Void
    let onPurchase: () -> Void

    var body: some View {
        VStack(alignment: .center, spacing: Spacing.lg) {
            AppMascot(pose: .celebrate, sizeDp: 96)

            VStack(spacing: Spacing.sm) {
                Text(title)
                    .font(AppType.title.weight(.bold))
                    .foregroundStyle(Color.appTextPrimary)
                    .multilineTextAlignment(.center)
                Text(bodyText)
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }

            usageCard

            if info.isMock {
                Text("PASS+ 회차는 Thunder 부터")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
                    .multilineTextAlignment(.center)
            }

            VStack(spacing: Spacing.sm) {
                AppButton(
                    title: "플랜 보기",
                    variant: .primary,
                    size: .regular,
                    action: onPurchase
                )
                AppButton(
                    title: "내일 다시 오기",
                    variant: .tertiary,
                    size: .regular,
                    action: onClose
                )
            }

            if !resetSubtitle.isEmpty {
                Text(resetSubtitle)
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .center)
        .background(Color.appPage)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Copy

    private var title: String {
        info.isMock ? "오늘 모의고사 1회 완료" : "오늘의 30문제 완주!"
    }

    private var bodyText: String {
        let noun = info.isMock ? "모의고사" : "문제"
        return "오늘 무료 \(noun)를 모두 풀었어요.\n플랜을 이용하면 매일 \(noun)를 더 풀 수 있어요."
    }

    private var unitLabel: String { info.isMock ? "회" : "문제" }

    // MARK: - Usage card

    private var usageCard: some View {
        HStack(spacing: Spacing.md) {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text("오늘 사용량")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
                HStack(alignment: .firstTextBaseline, spacing: Spacing.xs) {
                    Text("\(info.used)")
                        .font(AppType.monoNumericLarge.weight(.bold))
                        .foregroundStyle(Color.brandPrimary)
                    Text("/ \(info.limit) \(unitLabel)")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    /// 자정 리셋 안내. resetAt 파싱 무관하게 한 줄 고정 카피.
    private var resetSubtitle: String {
        "내일 0시에 다시 열려요"
    }

    /// 백엔드는 KST naive (`+09:00` 가정) → 표시용 "MM/dd HH:mm" 로 정규화.
    static func formatResetAt(_ raw: String) -> String? {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime]
        // KST naive 이므로 +09:00 붙여 ISO8601 으로 파싱.
        let withTZ = raw.hasSuffix("Z") || raw.contains("+") ? raw : "\(raw)+09:00"
        if let date = isoFormatter.date(from: withTZ) {
            let display = DateFormatter()
            display.locale = Locale(identifier: "ko_KR")
            display.timeZone = TimeZone(identifier: "Asia/Seoul")
            display.dateFormat = "M월 d일 HH:mm"
            return display.string(from: date)
        }
        // 파싱 실패 시 앞 16자 (YYYY-MM-DDTHH:mm) 잘라 안내.
        if raw.count >= 16 {
            return String(raw.prefix(16)).replacingOccurrences(of: "T", with: " ")
        }
        return nil
    }
}

#Preview("Question limit") {
    Color.clear.sheet(isPresented: .constant(true)) {
        QuotaPaywallView(
            info: QuotaPaywallInfo(
                code: "DAILY_QUESTION_LIMIT",
                used: 30,
                limit: 30,
                resetAt: "2026-05-22T00:00:00"
            ),
            onClose: {},
            onPurchase: {}
        )
    }
}

#Preview("Mock limit") {
    Color.clear.sheet(isPresented: .constant(true)) {
        QuotaPaywallView(
            info: QuotaPaywallInfo(
                code: "DAILY_MOCK_LIMIT",
                used: 1,
                limit: 1,
                resetAt: "2026-05-22T00:00:00"
            ),
            onClose: {},
            onPurchase: {}
        )
    }
}
