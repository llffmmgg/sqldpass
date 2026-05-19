import SwiftUI

/// 탭 진입 화면 공통 페이지 헤더 — iOS Large Title 패턴 (좌측 정렬 큰 제목 + 서브타이틀).
///
/// `.navigationTitle(.large)` 위에 자체 텍스트를 다시 그리지 않도록, 이 컴포넌트는
/// `.toolbar(.hidden, for: .navigationBar)` 와 함께 쓰거나, 시스템 nav 가 없는 ScrollView 내부에서
/// 페이지 머리에 단독으로 사용한다.
///
/// 디자인 출처: 871lJPyM 디자인 핸드오프 `screens.jsx` 의 `PageHeader`.
/// 폰트는 토큰 그대로 — `AppType.title` (Large Title 등가) + `AppType.callout muted`.
struct AppPageHeader: View {
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xxs) {
            Text(title)
                .font(AppType.title.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(AppType.callout)
                    .foregroundStyle(Color.appTextMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, Spacing.xs)
        .padding(.bottom, Spacing.base)
    }
}

#Preview("AppPageHeader — variants") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.lg) {
            AppPageHeader(title: "모의고사",
                          subtitle: "실전 감각을 유지할 수 있게 한 회차씩 정리했어요")
            AppPageHeader(title: "기출복원",
                          subtitle: "회차별 실제 시험과 동일한 구성으로 복원했어요")
            AppPageHeader(title: "실전 문제",
                          subtitle: "과목별 10문제 세트를 받아 한 문제씩 즉시 채점합니다")
            AppPageHeader(title: "내정보")
        }
        .padding(.horizontal, Spacing.base)
    }
    .background(Color.appPage)
}
