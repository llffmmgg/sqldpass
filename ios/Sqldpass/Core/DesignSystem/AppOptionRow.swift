import SwiftUI

/// 객관식 옵션 한 줄. 단일 채점/모의고사/리뷰 화면에서 공통으로 쓰는 primitive.
/// `Features/Solo/Components/SolveOptionRow.swift` 의 시각 정체성을 보존해 옮긴 버전.
///
/// 5가지 상태:
///  - idle
///  - selected
///  - revealed correct
///  - revealed selected & wrong
///  - revealed other (정답도 아니고 내가 고르지도 않은 선지)
///
/// 애니메이션:
///  - bounce 1.04 (spring) on becoming selected
///  - press scale 0.97 (Button style)
///  - shake-x 4pt × 5 (linear 60ms each) on becoming revealed-selected-wrong
///  - 200ms color crossfade on visual change
///
/// 옵션 본문은 frozen 한 `QuestionContentView` 로 그리며, 절대 `.lineLimit` 을 걸지 않는다.
enum AppOptionState: Equatable {
    case idle
    case selected
    case revealed(isCorrect: Bool, wasSelected: Bool)
}

struct AppOptionRow: View {
    let optionNumber: Int
    let optionText: String?
    let state: AppOptionState
    var onTap: () -> Void = {}
    var onDoubleTap: (() -> Void)? = nil

    @State private var shakeOffset: CGFloat = 0
    @State private var bounceScale: CGFloat = 1
    /// 손가락 down 상태 — `DragGesture(minimumDistance: 0)` 로 즉시 감지.
    /// 압축 scale 0.97 적용으로 "탭한 순간 카드가 반응" 감각.
    @GestureState private var isPressed: Bool = false

    /// 선택 / 공개 정보로부터 상태를 계산하는 헬퍼.
    static func appOptionStateOf(selected: Bool,
                                 revealed: Bool,
                                 isCorrectOption: Bool) -> AppOptionState {
        if revealed {
            return .revealed(isCorrect: isCorrectOption, wasSelected: selected)
        } else if selected {
            return .selected
        } else {
            return .idle
        }
    }

    private var isRevealed: Bool {
        if case .revealed = state { return true }
        return false
    }

    private var isSelectedIdle: Bool { state == .selected }

    private var isRevealedCorrect: Bool {
        if case .revealed(let correct, _) = state, correct { return true }
        return false
    }

    private var isRevealedSelectedWrong: Bool {
        if case .revealed(let correct, let wasSelected) = state,
           !correct, wasSelected {
            return true
        }
        return false
    }

    private var isRevealedOther: Bool {
        if case .revealed(let correct, let wasSelected) = state,
           !correct, !wasSelected {
            return true
        }
        return false
    }

    var body: some View {
        HStack(spacing: Spacing.sm) {
            Rectangle()
                .fill(barColor)
                .frame(width: barWidth)
                .frame(maxHeight: .infinity)
                .animation(.easeOut(duration: 0.2), value: barWidth)

            Spacer().frame(width: Spacing.xxs)

            // 스탬프 디스크 (옵션 번호)
            ZStack {
                Circle()
                    .fill(stampBackground)
                    .frame(width: 28, height: 28)
                Text("\(optionNumber)")
                    .font(AppType.monoNumeric.weight(.bold))
                    .foregroundStyle(stampForeground)
            }

            // 본문
            Group {
                if let text = optionText, !text.isEmpty {
                    QuestionContentView(text: text)
                } else {
                    Text("\(optionNumber)번")
                        .font(AppType.body)
                        .foregroundStyle(Color.appTextPrimary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, Spacing.sm)

            // 우측 상태 아이콘
            Image(systemName: trailingIcon)
                .font(AppType.body)
                .foregroundStyle(iconTint)
                .padding(.trailing, Spacing.md)
        }
        .frame(minHeight: 56)
        .background(containerColor)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(borderColor, lineWidth: borderWidth)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        .opacity(isRevealedOther ? 0.5 : 1.0)
        .scaleEffect(bounceScale * (isPressed && !isRevealed ? 0.97 : 1.0))
        .offset(x: shakeOffset)
        .contentShape(Rectangle())
        // minimumDistance 가 0 이면 ScrollView 의 drag 보다 먼저 잡혀서 풀이 화면 스크롤이 막힌다.
        // 10pt 이상 움직일 때만 press 상태로 — short tap 의 scale 효과는 살짝 양보.
        .simultaneousGesture(
            DragGesture(minimumDistance: 10)
                .updating($isPressed) { _, state, _ in state = true }
        )
        .simultaneousGesture(
            TapGesture(count: 2).onEnded {
                guard !isRevealed else { return }
                onDoubleTap?()
            }
        )
        .onTapGesture {
            guard !isRevealed else { return }
            onTap()
        }
        .onChange(of: state) { _, newValue in
            if case .revealed(let correct, let wasSelected) = newValue,
               !correct, wasSelected {
                animateShake()
            } else if newValue == .selected {
                animateBounce()
            }
        }
        .animation(.spring(response: 0.18, dampingFraction: 0.75), value: isPressed)
        .animation(.easeOut(duration: 0.2), value: state)
    }

    // MARK: - Animations

    private func animateShake() {
        Task { @MainActor in
            for offset in [CGFloat(4), -4, 3, -3, 0] {
                withAnimation(.linear(duration: 0.06)) {
                    shakeOffset = offset
                }
                try? await Task.sleep(nanoseconds: 60_000_000)
            }
        }
    }

    private func animateBounce() {
        withAnimation(.spring(response: 0.18, dampingFraction: 0.55)) {
            bounceScale = 1.04
        }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 140_000_000)
            withAnimation(.spring(response: 0.18, dampingFraction: 0.55)) {
                bounceScale = 1.0
            }
        }
    }

    // MARK: - Visual mapping

    private var borderColor: Color {
        if isRevealedCorrect       { return .semanticSuccess }
        if isRevealedSelectedWrong { return .semanticDanger }
        if isSelectedIdle          { return .brandPrimary }
        return .appBorder
    }

    private var borderWidth: CGFloat {
        if isRevealedCorrect || isRevealedSelectedWrong || isSelectedIdle {
            return 2
        }
        return 1
    }

    private var containerColor: Color {
        if isRevealedCorrect       { return Color.semanticSuccess.opacity(0.12) }
        if isRevealedSelectedWrong { return Color.semanticDanger.opacity(0.12) }
        if isSelectedIdle          { return Color.brandPrimary.opacity(0.10) }
        return .appSurface
    }

    private var barColor: Color {
        if isRevealedCorrect       { return .semanticSuccess }
        if isRevealedSelectedWrong { return .semanticDanger }
        if isSelectedIdle          { return .brandPrimary }
        return .clear
    }

    private var barWidth: CGFloat {
        if isRevealedCorrect || isRevealedSelectedWrong || isSelectedIdle {
            return 6
        }
        return 4
    }

    private var stampBackground: Color {
        if isRevealedCorrect       { return .semanticSuccess }
        if isRevealedSelectedWrong { return .semanticDanger }
        if isSelectedIdle          { return .brandPrimary }
        return .appElevated
    }

    private var stampForeground: Color {
        if isRevealedCorrect || isRevealedSelectedWrong || isSelectedIdle {
            return .white
        }
        return .appTextMuted
    }

    private var trailingIcon: String {
        if isRevealedCorrect || isSelectedIdle { return "checkmark.circle.fill" }
        if isRevealedSelectedWrong             { return "xmark.circle.fill" }
        return "circle"
    }

    private var iconTint: Color {
        if isRevealedCorrect       { return .semanticSuccess }
        if isRevealedSelectedWrong { return .semanticDanger }
        if isSelectedIdle          { return .brandPrimary }
        return .appTextSubtle
    }
}

// MARK: - Preview

#Preview("AppOptionRow — 5 states with long wrap") {
    let longText = """
SELECT department_id, COUNT(*) AS headcount FROM employee \
WHERE hire_date >= '2020-01-01' GROUP BY department_id \
HAVING COUNT(*) >= 5 ORDER BY headcount DESC; 라는 SQL 의 \
실행 결과로 옳은 것을 모두 고르시오. (긴 한국어 옵션이 자연스럽게 줄바꿈되어야 함)
"""

    return ScrollView {
        VStack(spacing: Spacing.md) {
            AppOptionRow(optionNumber: 1,
                         optionText: "idle 상태 — 평범한 후보 선지",
                         state: .idle)
            AppOptionRow(optionNumber: 2,
                         optionText: "selected 상태 — 사용자가 막 고른 직후",
                         state: .selected)
            AppOptionRow(optionNumber: 3,
                         optionText: "revealed correct — 정답 공개 후 정답인 선지",
                         state: .revealed(isCorrect: true, wasSelected: false))
            AppOptionRow(optionNumber: 4,
                         optionText: "revealed selected-wrong — 내가 골랐는데 오답",
                         state: .revealed(isCorrect: false, wasSelected: true))
            AppOptionRow(optionNumber: 5,
                         optionText: "revealed other — 정답도 아니고 내가 고르지도 않음",
                         state: .revealed(isCorrect: false, wasSelected: false))
            AppOptionRow(optionNumber: 6,
                         optionText: longText,
                         state: .idle)
        }
        .padding(Spacing.base)
    }
    .background(Color.appPage)
}
