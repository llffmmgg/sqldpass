import Foundation

/// Backend API base URL.
///
/// Simulator and debug builds use the production API by default so Google login
/// and payment-related flows can be tested without a local backend.
///
/// To point a local dev server, set `SQLDPASS_BACKEND_URL`.
/// Example:
/// `SIMCTL_CHILD_SQLDPASS_BACKEND_URL=http://127.0.0.1:8080`
enum APIEnvironment {
    static var current: URL {
        if let override = localBackendOverride, let url = URL(string: override) {
            return url
        }
        return URL(string: "https://www.sqldpass.com")!
    }

    static var localBackendOverride: String? {
        ProcessInfo.processInfo.environment["SQLDPASS_BACKEND_URL"]
    }
}
