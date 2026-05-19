import SwiftUI

/// 자격증 칩 가로 스크롤 행.
///
/// MockExams / PastExams / Home 의 cert 필터에 공통 사용. 각 칩은 좌측에 cert 액센트
/// 색의 작은 점 + 라벨 + 옵션 카운트 배지 구조. 선택 상태는 cert 액센트 톤으로 강조.
///
/// 디자인 출처: 871lJPyM `screens.jsx` 의 MockExam/PastExam cert filter.
struct AppCertChipRow<ID: Hashable>: View {
    struct Item: Identifiable {
        let id: ID
        let label: String
        /// 자격증 점/액센트 색. caller 가 도메인 매핑 후 `Color.certXxx` 주입.
        let dotColor: Color
        /// 옵션 — 우측 작은 숫자 배지. nil 이면 표시 안 함.
        let count: Int?
    }

    let items: [Item]
    let selectedId: ID?
    let onSelect: (ID) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(items) { item in
                    let selected = item.id == selectedId
                    Button {
                        onSelect(item.id)
                    } label: {
                        HStack(spacing: Spacing.xs) {
                            Circle()
                                .fill(item.dotColor)
                                .frame(width: 8, height: 8)
                            Text(item.label)
                                .font(AppType.footnote.weight(.semibold))
                                .foregroundStyle(selected ? item.dotColor : Color.appTextPrimary)
                            if let count = item.count {
                                Text("\(count)")
                                    .font(AppType.caption.monospacedDigit().weight(.semibold))
                                    .foregroundStyle(selected ? Color.brandPrimaryFG : Color.appTextMuted)
                                    .padding(.horizontal, Spacing.xs)
                                    .padding(.vertical, 1)
                                    .background(
                                        Capsule().fill(selected ? item.dotColor : Color.appSurface)
                                    )
                            }
                        }
                        .padding(.horizontal, Spacing.md)
                        .padding(.vertical, Spacing.sm)
                        .background(selected ? item.dotColor.opacity(0.12) : Color.appSurface)
                        .overlay(
                            Capsule()
                                .stroke(selected ? item.dotColor : Color.appBorder,
                                        lineWidth: selected ? 1.5 : 1)
                        )
                        .clipShape(Capsule())
                    }
                    .buttonStyle(AppChipButtonStyle())
                    .accessibilityLabel("\(item.label) 자격증 필터")
                }
            }
            .padding(.vertical, Spacing.xs)
        }
    }
}

#Preview("AppCertChipRow") {
    let items: [AppCertChipRow<String>.Item] = [
        .init(id: "sqld",   label: "SQLD",       dotColor: .certSQLD,             count: 6),
        .init(id: "epr",    label: "정처기 실기", dotColor: .certEngineerPractical, count: 12),
        .init(id: "ewr",    label: "정처기 필기", dotColor: .certEngineerWritten,   count: 8),
        .init(id: "cl1",    label: "컴활 1급",   dotColor: .certComputerL1,        count: 4),
        .init(id: "cl2",    label: "컴활 2급",   dotColor: .certComputerL2,        count: 2),
        .init(id: "adsp",   label: "ADsP",       dotColor: .certADSP,              count: nil),
    ]
    return AppCertChipRow(items: items, selectedId: "sqld", onSelect: { _ in })
        .padding(Spacing.base)
        .background(Color.appPage)
}
