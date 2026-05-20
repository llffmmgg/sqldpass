import SwiftUI

/// 문제 풀이 화면 하단 답안 카드 — 전체 문항 번호 5열 그리드.
///
/// 상태 분기 (좌→우 우선순위):
///  - **현재 문항**: `brandPrimary` 배경 + 흰 숫자.
///  - **답안 작성됨**: `brandPrimary` 0.15 배경 + brandPrimary 숫자.
///  - **미작성**: `appElevated` 배경 + muted 숫자.
///
/// 셀 탭 → `onTap(index)` 으로 viewModel.go(to:) 호출 권장. 제출 전 미응답 문항을 한눈에 추적.
struct SolveOMRSheet: View {
    let totalCount: Int
    let currentIndex: Int
    /// 답안이 작성된 문항의 0-base 인덱스 집합.
    let answeredIndices: Set<Int>
    let onTap: (Int) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 6), count: 5)

    private var answeredCount: Int { answeredIndices.count }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            header
            LazyVGrid(columns: columns, spacing: 6) {
                ForEach(0..<totalCount, id: \.self) { i in
                    cellButton(at: i)
                }
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                .stroke(Color.appBorder, lineWidth: 0.5)
        )
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline) {
            Text("답안 카드")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            Spacer()
            Text("\(answeredCount)/\(totalCount) 작성")
                .font(AppType.caption.weight(.semibold))
                .monospacedDigit()
                .foregroundStyle(Color.appTextMuted)
        }
    }

    @ViewBuilder
    private func cellButton(at i: Int) -> some View {
        let isCurrent = i == currentIndex
        let isAnswered = answeredIndices.contains(i)
        let bg: Color = isCurrent ? .brandPrimary
            : isAnswered ? Color.brandPrimary.opacity(0.15)
            : .appElevated
        let fg: Color = isCurrent ? .brandPrimaryFG
            : isAnswered ? .brandPrimary
            : .appTextMuted

        Button { onTap(i) } label: {
            Text("\(i + 1)")
                .font(AppType.footnote.weight(.semibold))
                .monospacedDigit()
                .foregroundStyle(fg)
                .frame(maxWidth: .infinity, minHeight: 32)
                .background(bg)
                .clipShape(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(i + 1)번 문제, \(isAnswered ? "작성됨" : "미작성")\(isCurrent ? ", 현재" : "")")
    }
}

#Preview {
    SolveOMRSheet(
        totalCount: 40,
        currentIndex: 7,
        answeredIndices: [0, 1, 2, 5, 7, 10, 11, 12, 15, 22],
        onTap: { _ in }
    )
    .padding()
    .background(Color.appPage)
}
