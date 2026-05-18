import SwiftUI

// MARK: - Public API

/// 진행률 알약의 강조 톤.
enum AppProgressAccent {
    case `default`
    case warning
    case danger

    var color: Color {
        switch self {
        case .default: return .brandPrimary
        case .warning: return .semanticWarning
        case .danger:  return .semanticDanger
        }
    }
}

/// 가로로 길쭉한 진행률 알약.
///
/// 구성: `[label?]   current/total   [▓▓▓▓░░░░]`
/// - 배경 `appSurface` + 1pt `appBorder`, 높이 32pt.
/// - 우측 64pt × 4pt 막대는 `current` 변경 시 spring-less easeOut 으로 부드럽게 채워진다.
struct AppProgressPill: View {
    let current: Int
    let total: Int
    var label: String? = nil
    var accent: AppProgressAccent = .default

    private var safeTotal: Int { max(total, 1) }
    private var clampedCurrent: Int { max(0, min(current, safeTotal)) }
    private var fraction: CGFloat {
        CGFloat(clampedCurrent) / CGFloat(safeTotal)
    }

    var body: some View {
        HStack(spacing: Spacing.sm) {
            if let label, !label.isEmpty {
                Text(label)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextMuted)
            }

            Text("\(clampedCurrent)/\(safeTotal)")
                .font(AppType.monoNumeric.weight(.semibold))
                .foregroundStyle(accent.color)

            ProgressBar(fraction: fraction, accent: accent)
                .frame(width: 64, height: 4)
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm - 2) // 6pt
        .frame(minHeight: 32)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.full, style: .continuous)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.full, style: .continuous))
    }
}

// MARK: - Internal: Animated progress bar

private struct ProgressBar: View {
    let fraction: CGFloat
    let accent: AppProgressAccent

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule(style: .continuous)
                    .fill(Color.appElevated)
                Capsule(style: .continuous)
                    .fill(accent.color)
                    .frame(width: max(0, min(1, fraction)) * geo.size.width)
                    .animation(.easeOut(duration: 0.28), value: fraction)
            }
        }
    }
}

// MARK: - Convenience: Timer pill

/// 시험/풀이 타이머 표시 전용. 남은 시간에 따라 자동 강조.
/// - `>= 60s` : default
/// - `< 60s`  : warning
/// - `<= 10s` : danger
///
/// 깜빡임 없음. 색상 전환만.
struct AppProgressPillTimer: View {
    let remainingSeconds: Int
    let totalSeconds: Int

    private var accent: AppProgressAccent {
        if remainingSeconds <= 10 { return .danger }
        if remainingSeconds < 60  { return .warning }
        return .default
    }

    private var formatted: String {
        let r = max(0, remainingSeconds)
        return String(format: "%02d:%02d", r / 60, r % 60)
    }

    private var fraction: CGFloat {
        let total = max(totalSeconds, 1)
        let r = max(0, min(remainingSeconds, total))
        return CGFloat(r) / CGFloat(total)
    }

    var body: some View {
        HStack(spacing: Spacing.sm) {
            Image(systemName: "clock")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(accent.color)

            Text(formatted)
                .font(AppType.monoNumeric.weight(.semibold))
                .foregroundStyle(accent.color)

            ProgressBar(fraction: fraction, accent: accent)
                .frame(width: 64, height: 4)
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm - 2)
        .frame(minHeight: 32)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.full, style: .continuous)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.full, style: .continuous))
    }
}

// MARK: - Preview

#Preview("AppProgressPill — variants") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.md) {
            Text("기본")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppProgressPill(current: 4, total: 10)

            Text("라벨 포함")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppProgressPill(current: 12, total: 20, label: "문항")

            Text("Warning")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppProgressPill(current: 18, total: 20, label: "남음", accent: .warning)

            Text("Danger")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppProgressPill(current: 19, total: 20, label: "위험", accent: .danger)

            Divider()
                .overlay(Color.appBorder)

            Text("Timer — 40s")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppProgressPillTimer(remainingSeconds: 40, totalSeconds: 90)

            Text("Timer — 8s")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)
            AppProgressPillTimer(remainingSeconds: 8, totalSeconds: 90)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}
