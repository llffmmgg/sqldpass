import SwiftUI

/// 실전 문제 탭 진입 화면 — 자격증 선택 후 과목 카드 → SoloSolveView push.
///
/// 정보 위계 (docs/MOBILE_UX_SPEC.md § 2.4):
///  1) 자격증 칩 (수평)
///  2) 과목 카드 그리드/리스트
///  3) 카드 탭 → SoloSolveScreen (1문 즉시 채점, 10문 세트)
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
            .navigationTitle("실전 문제")
            .navigationBarTitleDisplayMode(.inline)
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
                Text("자격증을 고르면 10문제 세트를 받아 한 문제씩 즉시 채점합니다.")
                    .font(AppType.callout)
                    .foregroundStyle(Color.appTextMuted)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, Spacing.base)

                if !viewModel.grouped.isEmpty {
                    SoloCertChipsRow(
                        groups: viewModel.grouped,
                        selectedParent: viewModel.selectedParent,
                        onSelect: { parent in viewModel.selectParent(parent) }
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
        } else {
            VStack(spacing: Spacing.md) {
                ForEach(viewModel.visibleGroups, id: \.parent.id) { group in
                    SubjectGroupCard(parent: group.parent.name, children: group.children)
                }
            }
        }
    }
}

// MARK: - 자격증 칩

private struct SoloCertChipsRow: View {
    let groups: [SoloHubViewModel.Group]
    let selectedParent: String?
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(groups, id: \.parent.id) { group in
                    SoloCertChip(
                        label: shortenCertName(group.parent.name),
                        dotColor: parentNameToCert(group.parent.name),
                        count: group.children.count,
                        selected: group.parent.name == selectedParent,
                        onTap: { onSelect(group.parent.name) }
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

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(spacing: Spacing.sm) {
                Circle()
                    .fill(parentNameToCert(parent))
                    .frame(width: 8, height: 8)
                Text(shortenCertName(parent))
                    .font(AppType.subheading)
                    .foregroundStyle(Color.appTextPrimary)
            }

            // 과목은 NavigationLink 로 push — SoloSolveView 가 NavigationStack 안에서 동작.
            VStack(spacing: Spacing.sm) {
                ForEach(children) { subject in
                    NavigationLink(value: subject) {
                        SubjectRow(name: subject.name)
                    }
                    .buttonStyle(.plain)
                }
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
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, Spacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appElevated)
        .clipShape(RoundedRectangle(cornerRadius: Radius.md))
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

// MARK: - parentName → 디자인 토큰 매핑

/// /api/subjects 의 parentName(예: "SQLD", "정보처리기사 실기") 을 자격증 점 색으로.
/// Android `SolveTab.parentNameToCert` 와 동일 규칙.
private func parentNameToCert(_ name: String) -> Color {
    if name.range(of: "SQLD", options: .caseInsensitive) != nil { return .certSQLD }
    if name.contains("실기") { return .certEngineerPractical }
    if name.contains("필기") && !name.contains("컴퓨터") { return .certEngineerWritten }
    if name.contains("컴퓨터활용능력 1") || name.contains("컴활 1") { return .certComputerL1 }
    if name.contains("컴퓨터활용능력 2") || name.contains("컴활 2") { return .certComputerL2 }
    if name.range(of: "ADsP", options: .caseInsensitive) != nil { return .certADSP }
    return .brandPrimary
}

/// 긴 한국어 자격증 명을 칩에 들어갈 짧은 라벨로.
/// Android `SolveTab.shortenCertName` 와 동일 규칙.
private func shortenCertName(_ name: String) -> String {
    if name.range(of: "SQLD", options: .caseInsensitive) != nil { return "SQLD" }
    if name.contains("정보처리기사 실기") { return "정처기 실기" }
    if name.contains("정보처리기사 필기") { return "정처기 필기" }
    if name.contains("컴퓨터활용능력 1") { return "컴활 1급" }
    if name.contains("컴퓨터활용능력 2") { return "컴활 2급" }
    if name.range(of: "ADsP", options: .caseInsensitive) != nil { return "ADsP" }
    return name
}

#Preview {
    NavigationStack {
        SoloHubView()
    }
}
