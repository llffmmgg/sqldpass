import Foundation
import Observation

/// 실전 문제 탭(SoloHubView) 의 과목 카탈로그 로더.
///
/// 백엔드 `/api/subjects` 응답은 트리(루트=과목 그룹, children=세부 과목) 형태이며
/// 루트가 "SQLD" 처럼 자격증 단위인 경우도 있고 "1과목 …", "2과목 …" 처럼 자격증 안의
/// 그룹 단위인 경우도 있다. 본 모델은 루트 이름을 `CertCatalog.slug` 로 매핑해
/// 자격증별로 묶어서 노출 — UI 는 자격증 칩 + 활성 자격증의 root 그룹들을 인라인 표시.
@MainActor
@Observable
final class SoloHubViewModel {

    private(set) var roots: [SubjectResponse] = []
    private(set) var isLoading: Bool = false
    private(set) var errorMessage: String? = nil

    /// 자격증 칩 선택값 — `CertCatalog.slug`. nil 이면 첫 자격증 자동 선택.
    var selectedSlug: String? = nil

    struct Group: Identifiable, Hashable {
        let slug: String                  // CertCatalog slug — sqld / engineer / ...
        let label: String                 // 표시 라벨 (CertCatalog.label)
        let roots: [SubjectResponse]      // 이 자격증에 속한 root 노드들 (1과목/2과목 등)
        var id: String { slug }

        /// 자격증 내부 children 총 개수 (그룹 안 세부 과목 합).
        var subjectCount: Int { roots.reduce(0) { $0 + $1.children.count } }
    }

    /// 자격증별로 묶인 그룹 — CertCatalog 순서. 응답에 없는 자격증은 제외.
    var grouped: [Group] {
        let byCert = Dictionary(grouping: roots) { Self.certSlug(fromRootName: $0.name) }
        return CertCatalog.all.compactMap { info -> Group? in
            guard let rs = byCert[info.slug], !rs.isEmpty else { return nil }
            return Group(
                slug: info.slug,
                label: info.label,
                roots: rs.sorted { $0.displayOrder < $1.displayOrder }
            )
        }
    }

    /// 활성 자격증 그룹 — selectedSlug 기준 1개.
    var activeGroup: Group? {
        guard let slug = selectedSlug else { return nil }
        return grouped.first { $0.slug == slug }
    }

    var subjectsCount: Int { roots.reduce(0) { $0 + $1.children.count } }

    func load(force: Bool = false) async {
        if !force, !roots.isEmpty { return }
        isLoading = true
        defer { isLoading = false }
        do {
            roots = try await SubjectService.all()
            errorMessage = nil
            if selectedSlug == nil {
                // SQLD 우선, 없으면 첫 자격증.
                selectedSlug = grouped.first(where: { $0.slug == "sqld" })?.slug
                    ?? grouped.first?.slug
            }
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func selectSlug(_ slug: String) {
        selectedSlug = slug
    }

    /// 백엔드 root.name → CertCatalog.slug 매핑. 매칭 안 되는 root 는 SQLD 로 fallback (웹 동작과 동일).
    /// 매핑 표는 `frontend/src/lib/cert-tokens.ts` 의 `ROOT_NAME_TO_CERT` 와 일치시킨다.
    static func certSlug(fromRootName name: String) -> String {
        switch name {
        case "정보처리기사 실기":           return "engineer"
        case "정보처리기사 필기":           return "engineer-written"
        case "컴퓨터활용능력 1급 필기":     return "computer-literacy-1"
        case "컴퓨터활용능력 2급 필기":     return "computer-literacy-2"
        case "데이터분석 준전문가(ADsP)":   return "adsp"
        case "SQLD":                       return "sqld"
        default:                           return "sqld"
        }
    }
}
