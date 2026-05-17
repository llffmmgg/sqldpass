import SwiftUI

struct MockExamDetailView: View {
    let examId: Int64

    @State private var detail: MockExamDetail?
    @State private var isLoading = true
    @State private var errorMessage: String?

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
            if detail != nil {
                Button {
                    // Phase 3.3 — 풀이 화면으로 이동 예정
                } label: {
                    Text("시험 시작하기")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
                .padding(.horizontal, Spacing.base)
                .padding(.bottom, Spacing.sm)
                .background(Color.appPage)
            }
        }
        .task {
            if detail == nil { await load() }
        }
    }

    // MARK: Sections

    private func header(for detail: MockExamDetail) -> some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text(detail.examType)
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
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
