import Foundation
import Security

/// Keychain 기반 단일 문자열 저장소. JWT 1개 보관 용도.
///
/// 외부 의존성 없이 Security framework 직접 사용. 백업/디바이스 잠금
/// 정책(`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) 적용해 다른
/// 기기 복원 시 토큰이 따라가지 않도록 한다.
struct KeychainStore {
    let service: String
    let account: String

    func save(_ value: String) throws {
        guard let data = value.data(using: .utf8) else {
            throw KeychainError.encoding
        }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        let attributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        let updateStatus = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if updateStatus == errSecItemNotFound {
            var addQuery = query
            addQuery.merge(attributes) { _, new in new }
            let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
            guard addStatus == errSecSuccess else {
                throw KeychainError.unhandled(status: addStatus)
            }
        } else if updateStatus != errSecSuccess {
            throw KeychainError.unhandled(status: updateStatus)
        }
    }

    func load() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess,
              let data = result as? Data,
              let value = String(data: data, encoding: .utf8) else {
            return nil
        }
        return value
    }

    func delete() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
    }
}

enum KeychainError: Error {
    case encoding
    case unhandled(status: OSStatus)
}
