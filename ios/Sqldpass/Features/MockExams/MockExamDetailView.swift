import SwiftUI

struct MockExamDetailView: View {
    let examId: Int64
    @Binding var path: NavigationPath

    @State private var detail: MockExamDetail?
    @State private var isLoading = true
    @State private var isStarting = false
    @State private var errorMessage: String?
    /// 서버 402 → 무료 회원 일일 모의고사 한도 도달 시 시트 노출.
    /// 자체 카운팅 금지 — 서버가 단일 진실.
    @State private var quotaPaywall: QuotaPaywallInfo?
    @State private var showPaywall = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                if let detail {
                    header(for: detail)
                    metadataGrid(for: detail)
                    subjectsSection(for: detail)
                }
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle(detail?.name ?? "모의고사")
        .navigationBarTitleDisplayMode(.inline)
        .overlay {
            if isLoading && detail == nil {
                ProgressView().controlSize(.large)
            } else if let errorMessage, detail == nil {
                ContentUnavailableView {
                    Label("불러오기 실패", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(errorMessage)
                } actions: {
                    Button("재시도") { Task { await load() } }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let detail {
                Button {
                    Task { await startExam() }
                } label: {
                    Text(isStarting ? "시작 중..." : "시험 시작하기")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
                .padding(.horizontal, Spacing.base)
                .padding(.bottom, Spacing.sm)
                .background(Color.appPage)
                .disabled(detail.questions.isEmpty || isStarting)
            }
        }
        .task {
            if detail == nil { await load() }
        }
        .sheet(item: $quotaPaywall) { info in
            QuotaPaywallView(
                info: info,
                onClose: {
                    quotaPaywall = nil
                    dismiss()
                },
                onPurchase: {
                    quotaPaywall = nil
                    showPaywall = true
                }
            )
        }
        .sheet(isPresented: $showPaywall) {
            PaywallView()
        }
        .hideCustomTabBar()
    }

    // MARK: Sections

    private func header(for detail: MockExamDetail) -> some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text(detail.typeLabel)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.brandPrimary)
            Text(detail.name)
                .font(AppType.title)
                .foregroundStyle(Color.appTextPrimary)
            if detail.expertVerified {
                Label("전문가 검수 완료", systemImage: "checkmark.seal.fill")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.brandPrimary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func metadataGrid(for detail: MockExamDetail) -> some View {
        HStack(spacing: Spacing.md) {
            metaCard(label: "문제 수", value: "\(detail.totalQuestions)")
            metaCard(label: "회차", value: "\(detail.sequence)회")
            if let year = detail.examYear, let round = detail.examRound {
                metaCard(label: "시행", value: "\(year).\(round)")
            }
        }
    }

    private func metaCard(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xxs) {
            Text(label)
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
            Text(value)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
        }
        .padding(Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private func subjectsSection(for detail: MockExamDetail) -> some View {
        let grouped = Dictionary(grouping: detail.questions, by: { $0.subjectName })
            .sorted { $0.key < $1.key }
        return VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("출제 과목")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)

            ForEach(grouped, id: \.key) { subject, items in
                HStack {
                    Image(systemName: "book.closed.fill")
                        .foregroundStyle(Color.appTextSubtle)
                    Text(subject)
                        .font(AppType.body)
                    Spacer()
                    Text("\(items.count)문제")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                }
                .padding(.vertical, Spacing.xs)
                if subject != grouped.last?.key {
                    Divider()
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

    // MARK: Loading

    private func load() async {
        isLoading = true
        do {
            detail = try await ExamService.detail(id: examId)
            errorMessage = nil
        } catch APIError.quotaExceeded(let code, let used, let limit, let resetAt) {
            quotaPaywall = QuotaPaywallInfo(code: code, used: used, limit: limit, resetAt: resetAt)
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func startExam() async {
        guard !isStarting else { return }
        isStarting = true
        defer { isStarting = false }
        do {
            let started = try await ExamService.start(id: examId)
            detail = started
            errorMessage = nil
            path.append(MockExamRoute.solve(examId: started.id, questions: started.questions))
        } catch APIError.quotaExceeded(let code, let used, let limit, let resetAt) {
            quotaPaywall = QuotaPaywallInfo(code: code, used: used, limit: limit, resetAt: resetAt)
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
