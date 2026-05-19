import SwiftUI

/// 모의고사 탭 — 자격증 필터 + 전체/완료/진행 요약 + 풀-탭 회차 카드.
///
/// 디자인 출처: 871lJPyM 디자인 핸드오프 `screens.jsx` 의 `MockExamScreen`.
/// 색·폰트·간격 토큰만 사용한다 (`Color.app* / brand* / semantic* / cert*`, `AppType.*`,
/// `Spacing.*`, `Radius.*`). 새 토큰을 추가하지 않는다.
struct MockExamsListView: View {
    @State private var viewModel = MockExamsViewModel()
    @State private var path = NavigationPath()
    @State private var selectedCertSlug: String? = nil

    var body: some View {
        NavigationStack(path: $path) {
            content
                .background(Color.appPage)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar(.hidden, for: .navigationBar)
                .refreshable {
                    await viewModel.load()
                }
                .task {
                    if viewModel.exams.isEmpty {
                        await viewModel.load()
                    }
                }
                .navigationDestination(for: MockExamRoute.self) { route in
                    switch route {
                    case .detail(let examId):
                        MockExamDetailView(examId: examId, path: $path)
                    case .solve(let examId, let questions):
                        SolveView(
                            viewModel: SolveViewModel(mockExamId: examId, questions: questions),
                            onSubmitted: { result in
                                path.append(MockExamRoute.result(result: result, questions: questions))
                            }
                        )
                    case .result(let result, let questions):
                        SolveResultView(
                            result: result,
                            questions: questions,
                            onDone: {
                                path = NavigationPath()
                            }
                        )
                    }
                }
        }
    }

    // MARK: - Derived data

    /// `viewModel.exams` 안에 등장하는 자격증 slug 들.
    private var availableCertSlugs: Set<String> {
        Set(viewModel.exams.map { slugFor(examType: $0.examType) })
    }

    /// 칩 항목. 현재 fetch 결과에 존재하는 자격증만 노출하되, 없으면 카탈로그 전체로 fallback.
    private var chipItems: [AppCertChipRow<String>.Item] {
        let counts = Dictionary(grouping: viewModel.exams) { slugFor(examType: $0.examType) }
            .mapValues { $0.count }
        let visibleSlugs = availableCertSlugs
        let source: [CertInfo] = visibleSlugs.isEmpty
            ? CertCatalog.all
            : CertCatalog.all.filter { visibleSlugs.contains($0.slug) }
        return source.map { info in
            AppCertChipRow<String>.Item(
                id: info.slug,
                label: info.label,
                dotColor: certColorOf(slug: info.slug),
                count: counts[info.slug]
            )
        }
    }

    /// 선택된 자격증 slug. 비어 있으면 칩의 첫 항목으로 자동 선택.
    private var activeSlug: String? {
        if let selectedCertSlug, chipItems.contains(where: { $0.id == selectedCertSlug }) {
            return selectedCertSlug
        }
        return chipItems.first?.id
    }

    /// 활성 자격증 필터를 적용한 회차 목록.
    private var filteredExams: [MockExamSummary] {
        guard let slug = activeSlug else { return viewModel.exams }
        return viewModel.exams.filter { slugFor(examType: $0.examType) == slug }
    }

    private var solvedCount: Int {
        filteredExams.filter { $0.solved }.count
    }

    /// "진행" = 풀이 기록은 있으나 아직 solved 아님.
    private var inProgressCount: Int {
        filteredExams.filter { $0.bestCorrectCount != nil && !$0.solved }.count
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.exams.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.exams.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("다시 시도") {
                    Task { await viewModel.load() }
                }
            }
        } else if viewModel.exams.isEmpty {
            ContentUnavailableView(
                "모의고사가 없어요",
                systemImage: "doc.text",
                description: Text("등록된 시험이 생기면 여기에 표시됩니다.")
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    AppPageHeader(
                        title: "모의고사",
                        subtitle: "실전 감각을 유지할 수 있게 한 회차씩 정리했어요"
                    )

                    AppCertChipRow(
                        items: chipItems,
                        selectedId: activeSlug,
                        onSelect: { slug in
                            selectedCertSlug = slug
                        }
                    )

                    summaryRow

                    LazyVStack(spacing: Spacing.md) {
                        ForEach(filteredExams) { exam in
                            let locked = exam.isPremium && !exam.purchased
                            Button {
                                guard !locked else { return }
                                path.append(MockExamRoute.detail(examId: exam.id))
                            } label: {
                                MockExamCard(exam: exam)
                            }
                            .buttonStyle(.plain)
                            .disabled(locked)
                        }
                    }
                }
                .padding(.horizontal, Spacing.base)
                .padding(.bottom, Spacing.xxl)
            }
        }
    }

    @ViewBuilder
    private var summaryRow: some View {
        HStack(spacing: Spacing.md) {
            summaryChunk(
                label: "전체",
                value: "\(filteredExams.count)회차",
                tint: Color.appTextPrimary
            )
            summaryDot
            summaryChunk(
                label: "완료",
                value: "\(solvedCount)",
                tint: Color.semanticSuccess
            )
            summaryDot
            summaryChunk(
                label: "진행",
                value: "\(inProgressCount)",
                tint: Color.brandPrimary
            )
            Spacer(minLength: 0)
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private func summaryChunk(label: String, value: String, tint: Color) -> some View {
        HStack(spacing: Spacing.xs) {
            Text(label)
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
            Text(value)
                .font(AppType.bodyEmph)
                .foregroundStyle(tint)
        }
    }

    private var summaryDot: some View {
        Circle()
            .fill(Color.appBorderStrong)
            .frame(width: 3, height: 3)
    }
}

// MARK: - Slug / cert mapping

/// `examType` (백엔드 ENUM 문자열) → CertCatalog slug.
private func slugFor(examType: String) -> String {
    switch examType {
    case "SQLD":                 return "sqld"
    case "ENGINEER_PRACTICAL":   return "engineer"
    case "ENGINEER_WRITTEN":     return "engineer-written"
    case "COMPUTER_LITERACY_ONE": return "computer-literacy-1"
    case "COMPUTER_LITERACY_TWO": return "computer-literacy-2"
    case "ADSP":                 return "adsp"
    default:                     return "sqld"
    }
}

/// 슬러그 → `AppCertBadge` 가 받는 `AppCert` enum.
private func appCertFor(slug: String) -> AppCert {
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

private struct MockExamCard: View {
    let exam: MockExamSummary

    private var locked: Bool {
        exam.isPremium && !exam.purchased
    }

    private var ringColor: Color {
        exam.solved ? .semanticSuccess : exam.typeAccentColor
    }

    private var sequenceLabel: String {
        "No.\(String(format: "%02d", exam.sequence))"
    }

    private var metaParts: [String] {
        var parts: [String] = []
        if let year = exam.examYear { parts.append("\(year)년") }
        parts.append("\(exam.totalQuestions)문제")
        if let difficulty = exam.difficultyLabel, !difficulty.isEmpty {
            parts.append(difficulty)
        }
        return parts
    }

    var body: some View {
        HStack(alignment: .top, spacing: Spacing.md) {
            leadingVisual
            middleColumn
            trailingTrailer
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(locked ? Color.appElevated : Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(locked ? Color.appBorderStrong : Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    @ViewBuilder
    private var leadingVisual: some View {
        if locked {
            ZStack {
                RoundedRectangle(cornerRadius: Radius.md)
                    .fill(Color.appElevated)
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.md)
                            .stroke(Color.appBorderStrong, lineWidth: 1)
                    )
                Image(systemName: "lock.fill")
                    .foregroundStyle(Color.semanticWarning)
            }
            .frame(width: 52, height: 52)
        } else {
            AppProgressRing(
                value: exam.bestCorrectCount ?? 0,
                total: max(exam.totalQuestions, 1),
                color: ringColor,
                size: 52,
                stroke: 4,
                label: exam.solved ? "✓" : nil
            )
        }
    }

    @ViewBuilder
    private var middleColumn: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack(spacing: Spacing.xs) {
                AppCertBadge(cert: appCertFor(slug: slugFor(examType: exam.examType)), size: .small)
                Text(sequenceLabel)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.appTextMuted)
                if exam.isPastExam {
                    Text("기출")
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.semanticInfo)
                        .padding(.horizontal, Spacing.sm)
                        .padding(.vertical, Spacing.xxs)
                        .background(Color.semanticInfo.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: Radius.full))
                }
            }

            Text(exam.name)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)

            metaRow
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var metaRow: some View {
        HStack(spacing: Spacing.xs) {
            ForEach(Array(metaParts.enumerated()), id: \.offset) { index, part in
                if index > 0 {
                    Circle()
                        .fill(Color.appBorderStrong)
                        .frame(width: 3, height: 3)
                }
                Text(part)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
            }
        }
    }

    @ViewBuilder
    private var trailingTrailer: some View {
        if locked {
            HStack(spacing: Spacing.xxs) {
                Image(systemName: "lock.fill")
                Text("PRO")
            }
            .font(AppType.caption.weight(.bold))
            .foregroundStyle(Color.semanticWarning)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, Spacing.xxs)
            .background(Color.semanticWarning.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: Radius.full))
        } else {
            Image(systemName: "chevron.right")
                .font(AppType.footnote.weight(.semibold))
                .foregroundStyle(Color.appTextSubtle)
                .padding(.top, Spacing.xs)
        }
    }
}

#Preview {
    MockExamsListView()
}
