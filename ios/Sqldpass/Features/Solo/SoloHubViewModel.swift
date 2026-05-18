import Foundation
import Observation

/// 실전 문제 탭(SoloHubView) 의 과목 카탈로그 로더.
///
/// Android 미러: `AppViewModel.loadSubjects()` + SolveTab 의 grouping.
@MainActor
@Observable
final class SoloHubViewModel {

    /// /api/subjects 응답 그대로.
    private(set) var subjects: [SubjectResponse] = []
    private(set) var isLoading: Bool = false
    private(set) var errorMessage: String? = nil

    /// 자격증 칩 선택값. nil 이면 "전체 자격증 보기" — 본 step 은 항상 첫 자격증 자동 선택.
    var selectedParent: String? = nil

    /// parentName 으로 그룹핑한 결과. 키 없는(parentName=nil) 과목은 "기타" 키로 묶는다.
    var grouped: [(parent: String, children: [SubjectResponse])] {
        // Dictionary 의 iteration 순서가 비결정적이므로 첫 출현 순서를 유지하기 위해
        // 한 번 순회하면서 parent 등장 순서를 보존.
        var order: [String] = []
        var bucket: [String: [SubjectResponse]] = [:]
        for s in subjects {
            let key = s.parentName ?? "기타"
            if bucket[key] == nil { order.append(key) }
            bucket[key, default: []].append(s)
        }
        return order.map { ($0, bucket[$0] ?? []) }
    }

    /// selectedParent 가 nil 이면 전체 그룹, 아니면 해당 parent 만.
    var visibleGroups: [(parent: String, children: [SubjectResponse])] {
        guard let selected = selectedParent else { return grouped }
        return grouped.filter { $0.parent == selected }
    }

    /// /api/subjects 1회 fetch. 이미 로드돼 있으면 skip.
    func load(force: Bool = false) async {
        if !force, !subjects.isEmpty { return }
        isLoading = true
        defer { isLoading = false }
        do {
            let list = try await SubjectService.all()
            subjects = list
            errorMessage = nil
            // 첫 자격증을 기본 선택값으로.
            if selectedParent == nil {
                selectedParent = grouped.first?.parent
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
