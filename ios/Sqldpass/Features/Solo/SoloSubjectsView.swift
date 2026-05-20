import SwiftUI

/// 실전 문제 — 자격증 선택 후 진입하는 과목 list 화면.
///
/// 백엔드 `SubjectResponse` 트리에서 자격증 root 의 `children` 을 그대로 노출.
/// 자식이 또 children 을 가지면(세부 과목) 그룹 헤더 + 하위 행으로 펼친다. 자식이 leaf 면
/// 단일 row 로 표시. row 탭 → `SoloSolveView` push (NavigationLink<SubjectResponse>).
struct SoloSubjectsView: View {
    let parent: SubjectResponse

    private var subjects: [SubjectResponse] {
        parent.children.sorted { $0.displayOrder < $1.displayOrder }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(parent.name)
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                    Text("과목을 선택해 한 세트씩 풀어봐요")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                }
                .padding(.top, Spacing.xs)

                if subjects.isEmpty {
                    Text("아직 등록된 과목이 없어요")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                        .padding(Spacing.lg)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.appSurface)
                        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
                } else {
                    VStack(spacing: Spacing.md) {
                        ForEach(subjects) { subject in
                            subjectBlock(for: subject)
                        }
                    }
                }
            }
            .padding(.horizontal, Spacing.base)
            .padding(.bottom, Spacing.xxl)
        }
        .background(Color.appPage)
        .navigationTitle(parent.name)
        .navigationBarTitleDisplayMode(.inline)
    }

    /// 과목 1개 — children 이 있으면 그룹 헤더 + 하위 세부과목 list,
    /// 없으면 단독 row (바로 풀이 진입).
    @ViewBuilder
    private func subjectBlock(for subject: SubjectResponse) -> some View {
        if subject.children.isEmpty {
            NavigationLink(value: subject) {
                SubjectRow(name: subject.name)
            }
            .buttonStyle(.plain)
            .background(Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        } else {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text(subject.name)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                    .padding(.horizontal, Spacing.xs)

                AppListGroupCard {
                    let leaves = subject.children.sorted { $0.displayOrder < $1.displayOrder }
                    ForEach(Array(leaves.enumerated()), id: \.element.id) { idx, leaf in
                        if idx > 0 { AppListGroupDivider() }
                        NavigationLink(value: leaf) {
                            SubjectRow(name: leaf.name)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
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
