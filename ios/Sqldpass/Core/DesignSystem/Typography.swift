import SwiftUI

/// iOS Dynamic Type 위주. 사용자의 시스템 글자 크기 설정을 존중하기 위해
/// 시스템 스타일(`.title`, `.body` 등)을 사용한다. 굳이 fixed-size font 를
/// 도입하지 말 것.
enum AppType {
    static let display     = Font.largeTitle.weight(.bold)   // 34+, Dashboard 헤더
    static let title       = Font.title.weight(.bold)        // 28+
    static let heading     = Font.title2.weight(.semibold)   // 22+
    static let subheading  = Font.title3.weight(.semibold)   // 20+
    static let bodyEmph    = Font.headline                   // 17 semibold
    static let body        = Font.body                       // 17
    static let callout     = Font.callout                    // 16
    static let footnote    = Font.footnote                   // 13
    static let caption     = Font.caption                    // 12

    // 점수/타이머 같은 숫자: 같은 너비, 흔들림 없음
    static let monoNumeric      = Font.body.monospacedDigit()
    static let monoNumericLarge = Font.title.monospacedDigit().weight(.semibold)
}
