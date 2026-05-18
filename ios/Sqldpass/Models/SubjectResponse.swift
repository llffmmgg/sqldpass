import Foundation

/// 백엔드 응답: GET /api/subjects (each)
///
/// Android 미러: `mobile/app/src/main/java/com/sqldpass/app/data/ApiModels.kt`
/// 의 `SubjectResponse` 와 동일 필드.
///
/// `parentName` 이 자격증 명(예: "SQLD", "정보처리기사 실기", "ADsP") 으로
/// 사용되어 SoloHubView 의 자격증 칩 그룹핑 키가 된다.
struct SubjectResponse: Codable, Equatable, Hashable, Identifiable {
    let id: Int64
    let name: String
    let parentId: Int64?
    let parentName: String?

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int64.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        parentId = try c.decodeIfPresent(Int64.self, forKey: .parentId)
        parentName = try c.decodeIfPresent(String.self, forKey: .parentName)
    }

    private enum CodingKeys: String, CodingKey {
        case id, name, parentId, parentName
    }
}
