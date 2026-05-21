import SwiftUI

/// 실전 문제 탭 진입 화면 — 자격증 선택 후 과목 카드 → SoloSolveView push.
///
/// 정보 위계 (docs/MOBILE_UX_SPEC.md § 2.4, 871lJPyM 핸드오프 PracticeScreen):
///  1) AppPageHeader (제목 + 서브타이틀)
///  2) 모드 토글 (10문제 세트 / 약점 보강 / 랜덤) — 첫 항목만 활성
///  3) 자격증 칩 (수평)
///  4) 자격증별 과목 그룹 (AppListGroupCard)
///  5) 카드 행 탭 → SoloSolveScreen (1문 즉시 채점, 10문 세트)
///
/// 백엔드 `/api/subjects` 는 트리(자격증 = 루트, 과목 = children) 응답이므로
/// 본 화면은 `SoloHubViewModel.grouped` 가 만들어 준 (parent root, children leaves)
/// 그룹을 그대로 렌더한다.
///
/// 인증: 앱 진입 시 이미 로그인 게이트 통과 — SoloHubView 도달 시점에는 로그인 보장.
/// fetch 실패(401 외 사유) 시 EmptyState 만 노출하고 강한 alert 는 띄우지 않는다.
struct SoloHubView: View {
    @State private var viewModel = SoloHubViewModel()

    var body: some View {
        content
            .background(Color.appPage)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
            .task {
                if viewModel.roots.isEmpty {
                    await viewModel.load()
                }
            }
            .refreshable {
                await viewModel.load(force: true)
            }
            .navigationDestination(for: SubjectResponse.self) { subject in
                SoloSolveView(
                    viewModel: SoloSolveViewModel(
                        subjectId: subject.id,
                        subjectName: subject.name
                    )
                )
            }
    }

    @ViewBuilder
    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                HStack(alignment: .top, spacing: Spacing.sm) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("실전 문제")
                            .font(AppType.bodyEmph)
                            .foregroundStyle(Color.appTextPrimary)
                        Text("오늘은 어떤 과목으로 한 세트 풀어볼까요?")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.appTextMuted)
                    }
                    Spacer(minLength: Spacing.sm)
                    // 무료 회원 일일 한도 — 활성 구독자는 자동으로 숨김.
                    AppQuotaBadge(kind: .question)
                }
                .padding(.top, Spacing.xs)

                if !viewModel.grouped.isEmpty {
                    SoloCertChipsRow(
                        groups: viewModel.grouped,
                        selectedSlug: viewModel.selectedSlug,
                        onSelect: { slug in viewModel.selectSlug(slug) }
                    )
                }

                listSection
            }
            .padding(.horizontal, Spacing.base)
            .padding(.bottom, Spacing.xxl)
        }
    }

    @ViewBuilder
    private var listSection: some View {
        if viewModel.isLoading && viewModel.roots.isEmpty {
            VStack(spacing: Spacing.md) {
                ForEach(0..<2, id: \.self) { _ in SkeletonSubjectGroup() }
            }
        } else if viewModel.roots.isEmpty || viewModel.subjectsCount == 0 {
            EmptyHint(message: viewModel.errorMessage)
        } else if let active = viewModel.activeGroup {
            VStack(spacing: Spacing.md) {
                ForEach(active.roots) { root in
                    SubjectGroupCard(
                        parent: root.name,
                        children: root.children,
                        dotColor: certColorOf(slug: active.slug)
                    )
                }
            }
        }
    }
}

// MARK: - 자격증 칩

private struct SoloCertChipsRow: View {
    let groups: [SoloHubViewModel.Group]
    let selectedSlug: String?
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(groups) { group in
                    SoloCertChip(
                        label: group.label,
                        dotColor: certColorOf(slug: group.slug),
                        count: group.subjectCount,
                        selected: group.slug == selectedSlug,
                        onTap: { onSelect(group.slug) }
                    )
                }
            }
            .padding(.vertical, Spacing.xs)
        }
    }
}

private struct SoloCertChip: View {
    let label: String
    let dotColor: Color
    let count: Int
    let selected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: Spacing.xs) {
                Circle()
                    .fill(dotColor)
                    .frame(width: 8, height: 8)
                Text(label)
                    .font(AppType.footnote.weight(.semibold))
                    .foregroundStyle(selected ? Color.brandPrimaryFG : Color.appTextPrimary)
                if count > 0 {
                    Text("\(count)")
                        .font(AppType.caption.monospacedDigit())
                        .foregroundStyle(selected ? Color.brandPrimaryFG.opacity(0.85) : Color.appTextMuted)
                }
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.sm)
            .background(selected ? Color.brandPrimary : Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.full)
                    .stroke(selected ? Color.brandPrimary : Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.full))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(label) 자격증 필터")
    }
}

// MARK: - 과목 그룹 카드

private struct SubjectGroupCard: View {
    let parent: String
    let children: [SubjectResponse]
    let dotColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            // group header — 자격증 색점 + root 이름 (예: "1과목 데이터 모델링의 이해") + 과목 수
            HStack(spacing: Spacing.sm) {
                Circle()
                    .fill(dotColor)
                    .frame(width: 8, height: 8)
                Text(parent)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Spacer(minLength: 0)
                Text("\(children.count)과목")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }
            .padding(.horizontal, Spacing.xs)

            // 과목은 NavigationLink 로 push — SoloSolveView 가 NavigationStack 안에서 동작.
            AppListGroupCard {
                ForEach(Array(children.enumerated()), id: \.element.id) { index, subject in
                    if index > 0 { AppListGroupDivider() }
                    NavigationLink(value: subject) {
                        SubjectRow(name: subject.name)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct SubjectRow: View {
    let name: String

    var body: some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: "play.circle.fill")
                .font(.title3)
                .foregroundStyle(Color.brandPrimary)
            Text(name)
                .font(AppType.body)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextSubtle)
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }
}

// MARK: - EmptyState / Skeleton

private struct EmptyHint: View {
    let message: String?

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("과목 정보를 가져오지 못했어요")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            Text(message ?? "잠시 후 다시 시도하거나 다른 탭을 이용해주세요.")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

private struct SkeletonSubjectGroup: View {
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            RoundedRectangle(cornerRadius: Radius.sm)
                .fill(Color.appElevated)
                .frame(width: 96, height: 18)
            ForEach(0..<3, id: \.self) { _ in
                RoundedRectangle(cornerRadius: Radius.md)
                    .fill(Color.appElevated)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}

#Preview {
    NavigationStack {
        SoloHubView()
    }
}
