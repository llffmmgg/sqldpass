import Foundation

/// 백엔드 응답: GET /api/subjects (각 원소).
///
/// 백엔드(`SubjectResponse.java`) 는 평면 리스트가 아니라 **트리** 를 내려준다.
/// `[ { id, name, displayOrder, children: [ { id, name, displayOrder, children: [] }, ... ] }, ... ]`
/// 루트 노드 = 자격증 (예: SQLD, 정보처리기사 실기), 자식 노드 = 과목.
///
/// 그룹핑/플래트닝 책임은 호출자(`SoloHubViewModel`) 에 있다.
struct SubjectResponse: Codable, Equatable, Hashable, Identifiable {
    let id: Int64
    let name: String
    let displayOrder: Int
    let children: [SubjectResponse]

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int64.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        displayOrder = (try c.decodeIfPresent(Int.self, forKey: .displayOrder)) ?? 0
        children = (try c.decodeIfPresent([SubjectResponse].self, forKey: .children)) ?? []
    }

    init(id: Int64, name: String, displayOrder: Int = 0, children: [SubjectResponse] = []) {
        self.id = id
        self.name = name
        self.displayOrder = displayOrder
        self.children = children
    }

    private enum CodingKeys: String, CodingKey {
        case id, name, displayOrder, children
    }
}
