import SwiftUI

/// 기출복원 탭 — 연도·회차별 기출 풀이 카탈로그.
///
/// 정보 위계 (docs/MOBILE_UX_SPEC.md § 2.3):
///  1) 자격증 칩 (수평 스크롤)
///  2) 회차 카드 리스트 (회차명·연도·회차·문항수·최고점수)
///  3) 카드 탭 → Runner (응시·채점)
///
/// 본 step 5 의 책임: 골격 + 자격증 칩 + 회차 카드 + EmptyState.
/// 카드 탭 시 실제 Runner 진입은 별 phase — 본 step 은 placeholder push.
///
/// Android 미러: `mobile/app/src/main/java/com/sqldpass/app/ui/pastexam/PastExamTab.kt`.
struct PastExamsListView: View {
    @State private var viewModel = PastExamsViewModel()

    var body: some View {
        content
            .background(Color.appPage)
            .navigationTitle("기출복원")
            .navigationBarTitleDisplayMode(.inline)
            .task {
                if viewModel.byCert[viewModel.selectedCert] == nil {
                    await viewModel.load(slug: viewModel.selectedCert)
                }
            }
            .refreshable {
                await viewModel.load(slug: viewModel.selectedCert, force: true)
            }
            .navigationDestination(for: PastExamSummary.self) { exam in
                PastExamRunnerView(viewModel: PastExamRunnerViewModel(examId: exam.id))
            }
    }

    @ViewBuilder
    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                CertChipsRow(
                    certs: CertCatalog.all,
                    selectedSlug: viewModel.selectedCert,
                    countByCert: viewModel.byCert.mapValues(\.count),
                    onSelect: { slug in
                        Task { await viewModel.selectCert(slug) }
                    }
                )
                .padding(.top, Spacing.base)

                listSection
            }
            .padding(.horizontal, Spacing.base)
            .padding(.bottom, Spacing.xxl)
        }
    }

    @ViewBuilder
    private var listSection: some View {
        let exams = viewModel.currentExams

        if viewModel.isLoading && exams.isEmpty {
            VStack(spacing: Spacing.md) {
                ForEach(0..<3, id: \.self) { _ in
                    SkeletonExamCard()
                }
            }
        } else if let errorMessage = viewModel.errorMessage, exams.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("다시 시도") {
                    Task { await viewModel.load(slug: viewModel.selectedCert, force: true) }
                }
            }
            .padding(.top, Spacing.xl)
        } else if exams.isEmpty {
            PastExamEmptyHint(slug: viewModel.selectedCert)
        } else {
            LazyVStack(spacing: Spacing.md) {
                ForEach(exams) { exam in
                    NavigationLink(value: exam) {
                        PastExamCard(exam: exam)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

// MARK: - 자격증 칩 행

/// 6종 자격증을 수평 스크롤 칩으로 노출. 선택 시 `onSelect(slug)` 호출.
/// 캐시된 회차 개수를 옆에 같이 표시.
private struct CertChipsRow: View {
    let certs: [CertInfo]
    let selectedSlug: String
    let countByCert: [String: Int]
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(certs) { info in
                    CertChip(
                        info: info,
                        selected: info.slug == selectedSlug,
                        count: countByCert[info.slug],
                        onTap: { onSelect(info.slug) }
                    )
                }
            }
            .padding(.vertical, Spacing.xs)
        }
    }
}

private struct CertChip: View {
    let info: CertInfo
    let selected: Bool
    let count: Int?
    let onTap: () -> Void

    private var dotColor: Color { certColorOf(slug: info.slug) }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: Spacing.xs) {
                Circle()
                    .fill(dotColor)
                    .frame(width: 8, height: 8)
                Text(info.label)
                    .font(AppType.footnote.weight(.semibold))
                    .foregroundStyle(selected ? Color.brandPrimaryFG : Color.appTextPrimary)
                if let count, count > 0 {
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
        .accessibilityLabel("\(info.label) 기출 회차 필터")
    }
}

// MARK: - 회차 카드

private struct PastExamCard: View {
    let exam: PastExamSummary

    private var meta: String {
        var parts: [String] = []
        if let year = exam.examYear { parts.append("\(year)년") }
        if let round = exam.examRound { parts.append("\(round)회") }
        parts.append("\(exam.totalQuestions)문제")
        return parts.joined(separator: " · ")
    }

    private var bestScoreLabel: String? {
        guard let correct = exam.bestCorrectCount else { return nil }
        let total = exam.bestTotalCount ?? exam.totalQuestions
        return "최고 \(correct)/\(total)"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(alignment: .center, spacing: Spacing.sm) {
                Text("기출")
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.semanticInfo)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xxs)
                    .background(Color.semanticInfo.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: Radius.full))

                Spacer()

                if exam.expertVerified {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundStyle(Color.brandPrimary)
                }
            }

            Text(exam.name)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: Spacing.md) {
                Label(meta, systemImage: "calendar")
                Spacer()
                if let best = bestScoreLabel {
                    Text(best)
                        .font(AppType.monoNumeric.weight(.semibold))
                        .foregroundStyle(Color.brandPrimary)
                }
            }
            .font(AppType.footnote)
            .foregroundStyle(Color.appTextMuted)

            HStack {
                Spacer()
                Label("기출 풀기", systemImage: "play.fill")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.brandPrimaryFG)
                Spacer()
            }
            .frame(height: 48)
            .background(Color.brandPrimary)
            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
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

// MARK: - EmptyState / Skeleton

private struct PastExamEmptyHint: View {
    let slug: String

    private var certLabel: String {
        CertCatalog.all.first(where: { $0.slug == slug })?.label ?? slug
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("\(certLabel) 기출 회차가 없어요")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            Text("곧 회차가 추가될 예정이에요. 다른 자격증을 선택해보세요.")
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

private struct SkeletonExamCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            RoundedRectangle(cornerRadius: Radius.sm)
                .fill(Color.appElevated)
                .frame(width: 64, height: 18)
            RoundedRectangle(cornerRadius: Radius.sm)
                .fill(Color.appElevated)
                .frame(maxWidth: .infinity, alignment: .leading)
                .frame(height: 20)
            RoundedRectangle(cornerRadius: Radius.sm)
                .fill(Color.appElevated)
                .frame(width: 140, height: 14)
            RoundedRectangle(cornerRadius: Radius.md)
                .fill(Color.appElevated)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
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
        PastExamsListView()
    }
}
