import SwiftUI

/// 모의고사 풀이 하단 액션 바 (Inked OMR 디자인 시스템).
///
/// - 일반 문항: `이전` (secondary) + `다음` (primary).
/// - 마지막 문항: `이전` (secondary) + `제출하기` (primary, 로딩 토글).
struct SolveActionBar: View {
    let canGoPrevious: Bool
    let canGoNext: Bool
    let isLastQuestion: Bool
    let isSubmitting: Bool
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onSubmit: () -> Void

    var body: some View {
        if isLastQuestion {
            AppBottomActionBar(
                primary: BottomAction(
                    title: "제출하기",
                    action: onSubmit,
                    isEnabled: !isSubmitting,
                    isLoading: isSubmitting,
                    variant: .primary
                ),
                secondary: BottomAction(
                    title: "이전",
                    action: onPrevious,
                    isEnabled: canGoPrevious,
                    variant: .secondary
                )
            )
        } else {
            AppBottomActionBar(
                primary: BottomAction(
                    title: "다음",
                    action: onNext,
                    isEnabled: canGoNext,
                    variant: .primary
                ),
                secondary: BottomAction(
                    title: "이전",
                    action: onPrevious,
                    isEnabled: canGoPrevious,
                    variant: .secondary
                )
            )
        }
    }
}
