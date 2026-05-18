import SwiftUI

/// 섹션 액션 (보조 텍스트 + 탭 핸들러). 라벨 + chevron 으로 표현된다.
struct SectionAction {
    let label: String
    let onTap: () -> Void
}

/// 화면/카드 내 섹션 헤더.
///
/// 구성:
///  - 옵션 eyebrow: `brandPrimary` 컬러, letter-spaced 1.2, 작은 caption.
///  - 제목: `AppType.heading`.
///  - 옵션 action: 라벨 + chevron, 우측 정렬.
///
/// 기존 `init(title:actionTitle:action:)` 호출부를 깨지 않도록 호환 이니셜라이저를 제공.
struct AppSectionHeader: View {
    let title: String
    var eyebrow: String? = nil
    var action: SectionAction? = nil

    /// 새 API.
    init(title: String,
         eyebrow: String? = nil,
         action: SectionAction? = nil) {
        self.title = title
        self.eyebrow = eyebrow
        self.action = action
    }

    /// 구 API 호환. `actionTitle + action` 조합을 `SectionAction` 으로 매핑한다.
    init(title: String,
         actionTitle: String?,
         action: (() -> Void)?) {
        self.title = title
        self.eyebrow = nil
        if let actionTitle, let action {
            self.action = SectionAction(label: actionTitle, onTap: action)
        } else {
            self.action = nil
        }
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Spacing.md) {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                if let eyebrow {
                    Text(eyebrow.uppercased())
                        .font(AppType.caption.weight(.semibold))
                        .tracking(1.2)
                        .foregroundStyle(Color.brandPrimary)
                }
                Text(title)
                    .font(AppType.heading)
                    .foregroundStyle(Color.appTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: Spacing.sm)

            if let action {
                Button(action: action.onTap) {
                    HStack(spacing: Spacing.xxs) {
                        Text(action.label)
                            .font(AppType.footnote.weight(.semibold))
                            .foregroundStyle(Color.brandPrimary)
                        Image(systemName: "chevron.right")
                            .font(AppType.footnote.weight(.semibold))
                            .foregroundStyle(Color.brandPrimary)
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Preview

#Preview("AppSectionHeader — variants") {
    ScrollView {
        VStack(alignment: .leading, spacing: Spacing.xl) {
            AppSectionHeader(title: "바로 시작")

            AppSectionHeader(title: "최근 점수 추이",
                             eyebrow: "Insights")

            AppSectionHeader(
                title: "과목별 오답률",
                eyebrow: "오답 분석",
                action: SectionAction(label: "전체 보기", onTap: {})
            )

            AppSectionHeader(title: "구버전 API 호환",
                             actionTitle: "더보기",
                             action: {})
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(Color.appPage)
}
