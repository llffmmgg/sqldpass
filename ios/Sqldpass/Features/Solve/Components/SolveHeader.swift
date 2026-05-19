import SwiftUI

/// 모의고사 진행 상단 헤더 (Inked OMR 디자인 시스템).
///
/// QuizScreen (`screens.jsx` ~492-623) 의 상단 chrome 패턴을 SwiftUI 로 옮긴 형태.
/// 구성:
///   row1: `current/total 미니 카운터  ......  타이머 칩(경과시간)`
///   row2: `AppSegmentedProgress` (현재 문항을 표시하는 셀 N 개)
///
/// 좌측 닫기 / 우측 북마크-플래그 아이콘은 호출 측(`SolveView`) 의 책임이며,
/// 이 컴포넌트는 가운데 가용 영역만 그린다.
struct SolveHeader: View {
    let progress: Double
    let currentIndex: Int
    let totalCount: Int
    let answeredCount: Int
    let elapsedSeconds: Int

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs + 2) {
            HStack(alignment: .center, spacing: Spacing.sm) {
                // 미니 카운터: "8 / 20" (현재만 강조, 분모는 muted)
                HStack(spacing: 4) {
                    Text("\(currentIndex + 1)")
                        .font(AppType.monoNumeric.weight(.semibold))
                        .foregroundStyle(Color.appTextPrimary)
                    Text("/ \(totalCount)")
                        .font(AppType.monoNumeric)
                        .foregroundStyle(Color.appTextMuted)
                }

                Spacer(minLength: Spacing.sm)

                // 우측 타이머 칩 — clock + mm:ss
                TimerChip(elapsedSeconds: elapsedSeconds)
            }

            AppSegmentedProgress(
                current: currentIndex + 1,
                total: totalCount,
                color: .brandPrimary
            )
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            "진행 \(currentIndex + 1)번째 문제, 총 \(totalCount)문제, 답안 \(answeredCount)개 작성, 경과 \(elapsedSeconds)초"
        )
    }
}

// MARK: - Timer chip

/// 모의고사 경과시간 칩.
///
/// `AppProgressPillTimer` 는 남은 시간 + 진행률 막대를 요구하지만, 모의고사 화면은
/// 경과 시간만 다루므로 단순 칩으로 분리. 색상은 `brandPrimary` 토큰을 그대로 사용
/// (디자인 가이드: 새 hex 도입 금지).
private struct TimerChip: View {
    let elapsedSeconds: Int

    private var formatted: String {
        let s = max(0, elapsedSeconds)
        return String(format: "%02d:%02d", s / 60, s % 60)
    }

    var body: some View {
        HStack(spacing: Spacing.xs + 1) {
            Image(systemName: "clock")
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.brandPrimary)
            Text(formatted)
                .font(AppType.caption.monospacedDigit().weight(.semibold))
                .foregroundStyle(Color.brandPrimary)
        }
        .padding(.horizontal, Spacing.sm + 1)
        .padding(.vertical, 3)
        .background(Color.brandPrimary.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: Radius.full, style: .continuous))
        .accessibilityLabel("경과 시간 \(formatted)")
    }
}
