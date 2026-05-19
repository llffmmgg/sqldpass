import SwiftUI

// MARK: - Public API

/// 문어 마스코트 포즈. Asset Catalog 이미지셋에 1:1 로 매핑된다.
/// 사용 가이드:
///  - `.greeting`   대시보드 인사
///  - `.focus`      집중 / 시험 진행
///  - `.celebrate`  성취 (focus 이미지 재사용)
///  - `.review`     오답/리뷰
///  - `.guide`      빈 상태 / 가이드
///  - `.onboarding` 온보딩 튜토리얼
enum AppMascotPose: String {
    case greeting
    case focus
    case celebrate
    case review
    case guide
    case onboarding

    /// Asset Catalog 이미지셋 이름.
    var assetName: String {
        switch self {
        case .greeting:   return "mascot_greeting"
        case .focus:      return "mascot_focus"
        case .celebrate:  return "mascot_focus"   // 의도적 재사용 — 동일한 자신감 표정
        case .review:     return "mascot_review"
        case .guide:      return "mascot_guide"
        case .onboarding: return "mascot_onboarding"
        }
    }
}

/// 문어 마스코트 일러스트.
///
/// - 등장 시 1회만 spring 으로 살짝 튕긴다 (0.92 → ~1.04 → 1.00).
/// - 등장 후 idle bobbing: y offset 0 ↔ -6pt, 위/아래 끝마다 짧게 멈춤.
/// - `accessibilityReduceMotion` 가 true 또는 `animateOnAppear == false` 면
///   등장 spring 과 idle bobbing 모두 비활성.
struct AppMascot: View {
    let pose: AppMascotPose
    var sizeDp: Int = 64
    var animateOnAppear: Bool = true

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var appeared: Bool = false

    private var side: CGFloat { CGFloat(sizeDp) }
    private var shouldBob: Bool { animateOnAppear && !reduceMotion }

    var body: some View {
        let base = Image(pose.assetName)
            .resizable()
            .scaledToFit()
            .frame(width: side, height: side)
            .scaleEffect(appeared ? 1.0 : 0.92)
            .animation(.spring(response: 0.45, dampingFraction: 0.6),
                       value: appeared)
            .onAppear {
                guard animateOnAppear else {
                    appeared = true
                    return
                }
                if reduceMotion {
                    // 동작 줄이기: 애니메이션 없이 최종 상태로 스냅
                    var tx = Transaction()
                    tx.disablesAnimations = true
                    withTransaction(tx) {
                        appeared = true
                    }
                } else {
                    appeared = true
                }
            }
            .accessibilityLabel(Text(pose.rawValue))

        if shouldBob {
            base.phaseAnimator(MascotBobPhase.allCases) { content, phase in
                content.offset(y: phase.offsetY)
            } animation: { phase in
                phase.animation
            }
        } else {
            base
        }
    }
}

// MARK: - Idle bobbing phases

private enum MascotBobPhase: CaseIterable {
    case rise, holdTop, fall, holdBottom

    var offsetY: CGFloat {
        switch self {
        case .rise, .holdTop:    return -6
        case .fall, .holdBottom: return 0
        }
    }

    var animation: Animation {
        switch self {
        case .rise:       return .easeInOut(duration: 1.0)
        case .holdTop:    return .linear(duration: 0.4)
        case .fall:       return .easeInOut(duration: 1.0)
        case .holdBottom: return .linear(duration: 0.4)
        }
    }
}

// MARK: - Preview

#Preview("AppMascot — 6 poses") {
    ScrollView {
        VStack(spacing: Spacing.lg) {
            ForEach([AppMascotPose.greeting,
                     .focus,
                     .celebrate,
                     .review,
                     .guide,
                     .onboarding], id: \.rawValue) { pose in
                VStack(spacing: Spacing.sm) {
                    AppMascot(pose: pose, sizeDp: 96)
                    Text(pose.rawValue)
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.appTextMuted)
                }
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity)
    }
    .background(Color.appPage)
}
