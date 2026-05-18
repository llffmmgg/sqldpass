import SwiftUI

/// 단일 채점 풀이의 옵션 1개 — 5가지 시각 상태.
///
/// Android `SolveOptionRow.kt` 와 동치. UI 표현은 SwiftUI native:
///  - press scale 0.97 (Button style 사용)
///  - selection bounce 1.04 (withAnimation spring)
///  - shake-x 0.3s (offset x animation)
struct SolveOptionRow: View {
    let optionNumber: Int
    let optionText: String?
    let selected: Bool
    let revealed: Bool
    let isCorrectOption: Bool
    let onTap: () -> Void
    let onDoubleTap: () -> Void

    @State private var shakeOffset: CGFloat = 0
    @State private var bounceScale: CGFloat = 1

    private var visual: OptionVisual {
        switch (revealed, isCorrectOption, selected) {
        case (true, true, _):  return .revealedCorrect
        case (true, false, true):  return .revealedSelectedWrong
        case (true, false, false): return .revealedOther
        case (false, _, true): return .selectedIdle
        default:               return .idle
        }
    }

    var body: some View {
        HStack(spacing: Spacing.sm) {
            // 좌측 액센트 바
            Rectangle()
                .fill(barColor)
                .frame(width: barWidth)
                .frame(maxHeight: .infinity)

            Spacer().frame(width: Spacing.xxs)

            // 번호 서클
            Text("\(optionNumber)")
                .font(AppType.bodyEmph)
                .foregroundStyle(circleFG)
                .frame(width: 28, height: 28)
                .background(Circle().fill(circleBG))

            // 옵션 본문
            Group {
                if let text = optionText, !text.isEmpty {
                    QuestionContentView(text: text)
                } else {
                    Text("\(optionNumber)번").font(AppType.body)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, Spacing.sm)

            // 우측 아이콘
            Image(systemName: trailingIcon)
                .foregroundStyle(iconTint)
                .padding(.trailing, Spacing.md)
        }
        .frame(minHeight: 64)
        .background(containerColor)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(borderColor, lineWidth: borderWidth)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        .opacity(visual == .revealedOther ? 0.5 : 1.0)
        .scaleEffect(bounceScale)
        .offset(x: shakeOffset)
        .contentShape(Rectangle())
        .onTapGesture(count: 2) {
            if !revealed { onDoubleTap() }
        }
        .onTapGesture {
            if !revealed { onTap() }
        }
        .onChange(of: visual) { _, newValue in
            if newValue == .revealedSelectedWrong {
                animateShake()
            }
        }
        .onChange(of: selected) { _, isSelected in
            if isSelected && !revealed { animateBounce() }
        }
        .animation(.easeOut(duration: 0.2), value: visual)
    }

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
        switch visual {
        case .idle, .revealedOther: .appBorder
        case .selectedIdle:         .brandPrimary
        case .revealedCorrect:      .semanticSuccess
        case .revealedSelectedWrong: .semanticDanger
        }
    }
    private var borderWidth: CGFloat {
        switch visual {
        case .selectedIdle, .revealedCorrect, .revealedSelectedWrong: 2
        default: 1
        }
    }
    private var containerColor: Color {
        switch visual {
        case .revealedCorrect:       Color.semanticSuccess.opacity(0.12)
        case .revealedSelectedWrong: Color.semanticDanger.opacity(0.12)
        case .selectedIdle:          Color.brandPrimary.opacity(0.10)
        default:                     .appSurface
        }
    }
    private var barColor: Color {
        switch visual {
        case .selectedIdle:          .brandPrimary
        case .revealedCorrect:       .semanticSuccess
        case .revealedSelectedWrong: .semanticDanger
        default:                     .clear
        }
    }
    private var barWidth: CGFloat {
        switch visual {
        case .idle, .revealedOther: 4
        default: 6
        }
    }
    private var circleBG: Color {
        switch visual {
        case .selectedIdle:          .brandPrimary
        case .revealedCorrect:       .semanticSuccess
        case .revealedSelectedWrong: .semanticDanger
        default:                     .appElevated
        }
    }
    private var circleFG: Color {
        switch visual {
        case .selectedIdle, .revealedCorrect, .revealedSelectedWrong: .white
        default: .appTextMuted
        }
    }
    private var trailingIcon: String {
        switch visual {
        case .revealedCorrect, .selectedIdle: "checkmark.circle.fill"
        case .revealedSelectedWrong:          "xmark.circle.fill"
        default:                              "circle"
        }
    }
    private var iconTint: Color {
        switch visual {
        case .selectedIdle:          .brandPrimary
        case .revealedCorrect:       .semanticSuccess
        case .revealedSelectedWrong: .semanticDanger
        default:                     .appTextSubtle
        }
    }
}

private enum OptionVisual: Equatable {
    case idle, selectedIdle, revealedCorrect, revealedSelectedWrong, revealedOther
}
