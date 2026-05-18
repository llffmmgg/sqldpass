import Foundation

/// 백엔드 베이스 URL.
///
/// 기본값은 운영 `https://sqldpass.com` — Debug 빌드도 동일. 로컬 dev 서버를
/// 가리키려면 `SQLDPASS_BACKEND_URL` 환경변수에 URL 지정.
/// (예: `SIMCTL_CHILD_SQLDPASS_BACKEND_URL=http://192.168.0.42:8080`)
enum APIEnvironment {
    static var current: URL {
        if let override = localBackendOverride, let url = URL(string: override) {
            return url
        }
        return URL(string: "https://sqldpass.com")!
    }

    /// 로컬 dev 서버 override (시뮬레이터 launch 환경변수)
    static var localBackendOverride: String? {
        ProcessInfo.processInfo.environment["SQLDPASS_BACKEND_URL"]
    }
}
