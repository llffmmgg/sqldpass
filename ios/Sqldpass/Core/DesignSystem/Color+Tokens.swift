import SwiftUI
import UIKit

extension Color {
    init(light: String, dark: String) {
        self.init(uiColor: UIColor { traits in
            let hex = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(hex: hex) ?? .black
        })
    }
}

extension Color {
    // MARK: Brand (sqldpass emerald)
    static let brandPrimary       = Color(light: "#24b47e", dark: "#3ecf8e")
    static let brandPrimaryHover  = Color(light: "#1fa374", dark: "#00c573")
    static let brandPrimaryFG     = Color(light: "#ffffff", dark: "#fafafa")
    static let brandCTABg         = Color(light: "#24b47e", dark: "#006239")
    static let brandCTABgHover    = Color(light: "#1fa374", dark: "#1f4b37")

    // MARK: Surfaces
    static let appPage            = Color(light: "#ffffff", dark: "#121212")
    static let appElevated        = Color(light: "#f4f4f4", dark: "#242424")
    static let appSurface         = Color(light: "#ffffff", dark: "#2e2e2e")
    static let appSurfaceHover    = Color(light: "#f4f4f4", dark: "#242424")

    // MARK: Borders
    static let appBorder          = Color(light: "#e5e5e5", dark: "#393939")
    static let appBorderStrong    = Color(light: "#c4c4c4", dark: "#4d4d4d")

    // MARK: Text
    static let appTextPrimary     = Color(light: "#181818", dark: "#fafafa")
    static let appTextMuted       = Color(light: "#666666", dark: "#b4b4b4")
    static let appTextSubtle      = Color(light: "#8a8a8a", dark: "#898989")

    // MARK: Semantic
    static let semanticDanger     = Color(light: "#ef4444", dark: "#f63737")
    static let semanticWarning    = Color(light: "#d97706", dark: "#ffb800")
    static let semanticSuccess    = Color(light: "#00997a", dark: "#00b8a3")
    static let semanticInfo       = Color(light: "#0a84ff", dark: "#0a84ff")

    // MARK: Certification accents
    static let certSQLD                = Color(light: "#f59e0b", dark: "#f59e0b")
    static let certEngineerPractical   = Color(light: "#2dbb7a", dark: "#2dbb7a")
    static let certEngineerWritten     = Color(light: "#f43f5e", dark: "#f43f5e")
    static let certComputerL1          = Color(light: "#0ea5e9", dark: "#0ea5e9")
    static let certComputerL2          = Color(light: "#6366f1", dark: "#6366f1")
    static let certADSP                = Color(light: "#14b8a6", dark: "#14b8a6")

    // MARK: Code surface (다크톤 고정 — 라이트/다크 무관, 웹 zinc-900 톤과 매칭)
    static let appCodeSurface   = Color(light: "#1e1e1e", dark: "#1e1e1e")
    static let appCodeHeader    = Color(light: "#2a2a2a", dark: "#2a2a2a")
    static let appCodeBorder    = Color(light: "#3a3a3a", dark: "#3a3a3a")
    static let appCodeText      = Color(light: "#e6e6e6", dark: "#e6e6e6")
    static let appCodeInline    = Color(light: "#1f1f1f", dark: "#1f1f1f")
    static let appCodeInlineFG  = Color(light: "#fcd34d", dark: "#fcd34d")
}
