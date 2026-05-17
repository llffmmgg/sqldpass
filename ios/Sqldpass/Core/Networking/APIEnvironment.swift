import Foundation

/// 빌드 컨피그별 백엔드 베이스 URL.
///
/// Debug → `http://localhost:8080` (시뮬레이터에서 맥 로컬 백엔드 직통)
/// Release → `https://sqldpass.com`
///
/// 실기기에서 로컬 백엔드를 쓰려면 `localBackendOverride` 에 맥의 LAN IP 를
/// 일시 지정한다 (예: `http://192.168.0.42:8080`). 이때 Info.plist 의 ATS 예외 필요.
enum APIEnvironment {
    static var current: URL {
        if let override = localBackendOverride, let url = URL(string: override) {
            return url
        }
        #if DEBUG
        return URL(string: "http://localhost:8080")!
        #else
        return URL(string: "https://sqldpass.com")!
        #endif
    }

    /// 실기기 빌드 시 임시로 맥 LAN IP 지정 가능.
    static var localBackendOverride: String? {
        ProcessInfo.processInfo.environment["SQLDPASS_BACKEND_URL"]
    }
}
