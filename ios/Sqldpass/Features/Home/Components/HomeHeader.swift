import SwiftUI

/// 고정 헤더 — "문어CBT" + 우측 SF Symbols 액션 3개.
/// status bar 까지 `brandPrimary` 배경을 확장한다. 스크롤되지 않는 영역.
struct HomeBrandHeader: View {
    var onPlanTap: () -> Void = {}
    var onNotificationTap: () -> Void = {}
    var onProfileTap: () -> Void = {}

    var body: some View {
        HStack(spacing: 0) {
            Text("문어CBT")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(Color.brandPrimaryFG)

            Spacer(minLength: Spacing.md)

            HStack(spacing: Spacing.lg) {
                symbolButton("sparkles", accessibility: "결제 플랜", tint: .yellow, action: onPlanTap)
                symbolButton("bell", accessibility: "알림", action: onNotificationTap)
                symbolButton("person.crop.circle", accessibility: "내 정보", action: onProfileTap)
            }
        }
        .padding(.horizontal, Spacing.base)
        .padding(.top, Spacing.sm)
        .padding(.bottom, Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.brandPrimary.ignoresSafeArea(edges: .top))
    }

    private func symbolButton(
        _ systemName: String,
        accessibility: String,
        tint: Color = .brandPrimaryFG,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 32, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibility)
    }
}

/// 스크롤 가능한 본문 첫 섹션 — D-day, 동기부여 카피, streak 멘트.
/// 헤더와 같은 `brandPrimary` 배경을 갖되, 하단 좌/우 모서리만 라운드 마감하여
/// 흰 본문(`appPage`)과 시각적으로 자연스럽게 연결된다.
struct HomeDDayBanner: View {
    let dDayText: String
    let daysRemaining: Int
    let nickname: String
    let streakDays: Int

    var onEditExamDateTap: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            dDayRow
                .padding(.bottom, Spacing.sm)
            messageBlock
                .padding(.bottom, Spacing.md)
            streakLine
        }
        .padding(.horizontal, Spacing.base)
        .padding(.top, Spacing.sm)
        .padding(.bottom, Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            UnevenRoundedRectangle(
                cornerRadii: .init(bottomLeading: 28, bottomTrailing: 28),
                style: .continuous
            )
            .fill(Color.brandPrimary)
        )
    }

    private var dDayRow: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(dDayText)
                .font(.system(size: 36, weight: .heavy, design: .rounded))
                .foregroundStyle(Color.brandPrimaryFG)
                .monospacedDigit()

            Spacer()

            Button("수정", action: onEditExamDateTap)
                .font(AppType.footnote.weight(.semibold))
                .foregroundStyle(Color.brandPrimaryFG.opacity(0.85))
                .buttonStyle(.plain)
                .accessibilityLabel("시험일 수정")
        }
    }

    private var messageBlock: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("시험날까지 \(daysRemaining)일 남았어요!")
                .font(AppType.body.weight(.semibold))
                .foregroundStyle(Color.brandPrimaryFG)
            Text("모의고사를 풀고 합격확률을 높혀봐요")
                .font(AppType.footnote)
                .foregroundStyle(Color.brandPrimaryFG.opacity(0.88))
        }
    }

    private var streakLine: some View {
        streakText
            .font(AppType.footnote)
            .foregroundStyle(Color.brandPrimaryFG.opacity(0.92))
    }

    /// streak 일수에 따라 톤을 분기.
    private var streakText: Text {
        if streakDays >= 7 {
            return Text(nickname).bold()
                 + Text("님, ")
                 + Text("\(streakDays)일 연속 학습").bold()
                 + Text(" 중이에요. 페이스 정말 좋아요!")
        } else if streakDays >= 1 {
            return Text(nickname).bold()
                 + Text("님, 오늘까지 ")
                 + Text("\(streakDays)일 연속").bold()
                 + Text(" 함께하고 있어요")
        } else {
            return Text(nickname).bold()
                 + Text("님, 오늘 한 문제부터 다시 시작해볼까요?")
        }
    }
}

#Preview {
    VStack(spacing: 0) {
        HomeBrandHeader()
        ScrollView {
            VStack(spacing: 0) {
                HomeDDayBanner(
                    dDayText: "D-300",
                    daysRemaining: 300,
                    nickname: "경북대화이팅",
                    streakDays: 9
                )
                Color.appPage.frame(height: 400)
            }
        }
        .background(Color.appPage)
    }
    .ignoresSafeArea(edges: .top)
}
