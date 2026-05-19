import SwiftUI

/// 기출복원 탭 — 연도·회차별 기출 풀이 카탈로그.
///
/// 정보 위계 (docs/MOBILE_UX_SPEC.md § 2.3):
///  1) 페이지 헤더 (`AppPageHeader`)
///  2) 자격증 칩 (`AppCertChipRow`, slug 별 회차 수 배지)
///  3) 회차 카드 리스트 — 좌측 `AppProgressRing`, 중앙 cert 뱃지 + 타이틀 + 메타, 우측 chevron
///  4) 카드 탭 → `PastExamRunnerView` (응시·채점)
///
/// 색·폰트·간격 토큰만 사용한다 (`Color.app* / brand* / semantic* / cert*`, `AppType.*`,
/// `Spacing.*`, `Radius.*`). 새 토큰을 추가하지 않는다.
///
/// Android 미러: `mobile/app/src/main/java/com/sqldpass/app/ui/pastexam/PastExamTab.kt`.
struct PastExamsListView: View {
    @State private var viewModel = PastExamsViewModel()

    var body: some View {
        content
            .background(Color.appPage)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
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

    private var chipItems: [AppCertChipRow<String>.Item] {
        CertCatalog.all.map { info in
            AppCertChipRow<String>.Item(
                id: info.slug,
                label: info.label,
                dotColor: certColorOf(slug: info.slug),
                count: viewModel.byCert[info.slug]?.count
            )
        }
    }

    @ViewBuilder
    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                AppPageHeader(
                    title: "기출복원",
                    subtitle: "회차별 실제 시험과 동일한 구성으로 복원했어요"
                )

                AppCertChipRow(
                    items: chipItems,
                    selectedId: viewModel.selectedCert,
                    onSelect: { slug in
                        Task { await viewModel.selectCert(slug) }
                    }
                )

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
                ForEach(Array(exams.enumerated()), id: \.element.id) { index, exam in
                    NavigationLink(value: exam) {
                        PastExamCard(exam: exam, isNew: index == 0)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

// MARK: - 슬러그 → AppCert 매핑

/// 슬러그 → `AppCertBadge` 가 받는 `AppCert` enum.
private func appCertFor(slug: String?) -> AppCert {
    switch slug {
    case "sqld":                 return .sqld
    case "engineer":             return .engineerPractical
    case "engineer-written":     return .engineerWritten
    case "computer-literacy-1":  return .cl1
    case "computer-literacy-2":  return .cl2
    case "adsp":                 return .adsp
    default:                     return .sqld
    }
}

// MARK: - 회차 카드

private struct PastExamCard: View {
    let exam: PastExamSummary
    let isNew: Bool

    private var certColor: Color { certColorOf(slug: exam.certSlug ?? "sqld") }

    private var ringColor: Color {
        exam.solved ? .semanticSuccess : certColor
    }

    private var ringTotal: Int {
        max(exam.bestTotalCount ?? exam.totalQuestions, 1)
    }

    private var ringValue: Int {
        max(0, min(exam.bestCorrectCount ?? 0, ringTotal))
    }

    private var ringLabel: String {
        if exam.solved { return "✓" }
        let denom = max(exam.totalQuestions, 1)
        let pct = Int((Double(exam.bestCorrectCount ?? 0) / Double(denom) * 100).rounded())
        return "\(pct)%"
    }

    private var metaText: String {
        var parts: [String] = []
        if let year = exam.examYear { parts.append("\(year)년") }
        if let round = exam.examRound { parts.append("\(round)회") }
        parts.append("\(exam.totalQuestions)문제")
        return parts.joined(separator: " · ")
    }

    var body: some View {
        HStack(alignment: .top, spacing: Spacing.md) {
            AppProgressRing(
                value: ringValue,
                total: ringTotal,
                color: ringColor,
                size: 56,
                stroke: 4,
                label: ringLabel
            )

            VStack(alignment: .leading, spacing: Spacing.xs) {
                HStack(spacing: Spacing.xs) {
                    AppCertBadge(cert: appCertFor(slug: exam.certSlug), size: .small)
                    if isNew {
                        Text("NEW")
                            .font(AppType.caption.weight(.bold))
                            .foregroundStyle(Color.brandPrimaryFG)
                            .padding(.horizontal, Spacing.sm)
                            .padding(.vertical, Spacing.xxs)
                            .background(certColor)
                            .clipShape(RoundedRectangle(cornerRadius: Radius.full))
                    }
                    if exam.expertVerified {
                        Image(systemName: "checkmark.seal.fill")
                            .font(AppType.footnote)
                            .foregroundStyle(Color.brandPrimary)
                    }
                }

                Text(exam.name)
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)

                HStack(spacing: Spacing.xs) {
                    Label(metaText, systemImage: "calendar")
                        .labelStyle(.titleAndIcon)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                    if exam.solved {
                        Circle()
                            .fill(Color.appBorderStrong)
                            .frame(width: 3, height: 3)
                        Text("완료")
                            .font(AppType.footnote.weight(.semibold))
                            .foregroundStyle(Color.semanticSuccess)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "chevron.right")
                .font(AppType.footnote.weight(.semibold))
                .foregroundStyle(Color.appTextSubtle)
                .padding(.top, Spacing.xs)
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            ZStack {
                Color.appSurface
                if isNew {
                    certColor.opacity(0.08)
                }
            }
        )
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(isNew ? certColor.opacity(0.4) : Color.appBorder, lineWidth: 1)
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
        HStack(alignment: .top, spacing: Spacing.md) {
            Circle()
                .fill(Color.appElevated)
                .frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: Spacing.sm) {
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
            }
            .frame(maxWidth: .infinity, alignment: .leading)
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
