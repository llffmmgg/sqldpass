import Foundation
import Observation

/// 실전 문제 탭(SoloHubView) 의 과목 카탈로그 로더.
///
/// 백엔드 `/api/subjects` 응답은 트리(루트=자격증, children=과목) 형태.
/// 본 모델은 트리를 받아 `[ (parent, children) ]` 로 노출 — UI 가 평탄화 책임을 갖지 않도록.
@MainActor
@Observable
final class SoloHubViewModel {

    /// 백엔드 응답 그대로 (루트 노드 = 자격증).
    private(set) var roots: [SubjectResponse] = []
    private(set) var isLoading: Bool = false
    private(set) var errorMessage: String? = nil

    /// 자격증 칩 선택값. nil 이면 전체. 본 step 은 첫 자격증 자동 선택.
    var selectedParent: String? = nil

    /// 한 자격증의 (자격증 + 그 과목들) 한 묶음.
    /// 튜플 대신 struct 로 둠 — View 가 `id: \.parent.id` 형태로 keypath 를 안전하게 잡도록.
    struct Group: Identifiable {
        let parent: SubjectResponse
        let children: [SubjectResponse]
        var id: Int64 { parent.id }
    }

    /// 자격증 그룹 목록 — 루트 노드를 displayOrder 순으로 정렬.
    var grouped: [Group] {
        roots
            .sorted { $0.displayOrder < $1.displayOrder }
            .map { Group(parent: $0, children: $0.children.sorted { $0.displayOrder < $1.displayOrder }) }
    }

    /// 칩 필터 적용된 그룹.
    var visibleGroups: [Group] {
        guard let selected = selectedParent else { return grouped }
        return grouped.filter { $0.parent.name == selected }
    }

    /// 화면에 노출되는 과목 카드의 평탄화된 합(EmptyHint 분기용).
    var subjectsCount: Int {
        roots.reduce(into: 0) { $0 += $1.children.count }
    }

    /// `/api/subjects` 1회 fetch. 이미 로드돼 있으면 skip.
    func load(force: Bool = false) async {
        if !force, !roots.isEmpty { return }
        isLoading = true
        defer { isLoading = false }
        do {
            roots = try await SubjectService.all()
            errorMessage = nil
            if selectedParent == nil {
                selectedParent = grouped.first?.parent.name
            }
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func selectParent(_ parent: String) {
        selectedParent = parent
    }
}
