import SwiftUI
import UIKit

/// 통합 텍스트 인풋. 라벨/헬퍼/에러를 한 묶음으로 그리고, 포커스/에러 시 보더 색을 바꾼다.
/// SwiftUI 기본 chrome 을 쓰지 않는다 — `.textFieldStyle(.plain)` + 자체 컨테이너.
struct AppTextField: View {
    @Binding var text: String
    let label: String
    var placeholder: String = ""
    var helper: String? = nil
    var error: String? = nil
    var isEnabled: Bool = true
    var isSecure: Bool = false
    var keyboardType: UIKeyboardType = .default
    var leadingSystemImage: String? = nil
    var onSubmit: (() -> Void)? = nil

    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(label)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextMuted)

            HStack(spacing: Spacing.sm) {
                if let leadingSystemImage {
                    Image(systemName: leadingSystemImage)
                        .font(AppType.body)
                        .foregroundStyle(iconColor)
                }
                inputField
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(minHeight: 48)
            .background(Color.appElevated)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.md)
                    .stroke(borderColor, lineWidth: borderWidth)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
            .contentShape(Rectangle())
            .onTapGesture { isFocused = true }

            if let error {
                Text(error)
                    .font(AppType.caption)
                    .foregroundStyle(Color.semanticDanger)
                    .fixedSize(horizontal: false, vertical: true)
            } else if let helper {
                Text(helper)
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .opacity(isEnabled ? 1.0 : 0.45)
        .animation(.easeOut(duration: 0.15), value: isFocused)
        .animation(.easeOut(duration: 0.15), value: error)
    }

    @ViewBuilder
    private var inputField: some View {
        Group {
            if isSecure {
                SecureField(placeholder, text: $text)
            } else {
                TextField(placeholder, text: $text)
                    .keyboardType(keyboardType)
            }
        }
        .textFieldStyle(.plain)
        .focused($isFocused)
        .font(AppType.body)
        .foregroundStyle(Color.appTextPrimary)
        .tint(Color.brandPrimary)
        .disabled(!isEnabled)
        .submitLabel(onSubmit == nil ? .return : .done)
        .onSubmit { onSubmit?() }
    }

    // MARK: - Colors

    private var borderColor: Color {
        if error != nil { return .semanticDanger }
        if isFocused    { return .brandPrimary }
        return .appBorder
    }
    private var borderWidth: CGFloat {
        if error != nil || isFocused { return 1.5 }
        return 1
    }
    private var iconColor: Color {
        if error != nil { return .semanticDanger }
        if isFocused    { return .brandPrimary }
        return .appTextMuted
    }
}

// MARK: - Preview

#Preview("AppTextField — states") {
    AppTextFieldPreviewHost()
}

private struct AppTextFieldPreviewHost: View {
    @State private var empty: String = ""
    @State private var filled: String = "heehun@example.com"
    @State private var errored: String = "abc"
    @State private var pw: String = "secret"
    @State private var disabled: String = "변경 불가"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                AppTextField(text: $empty,
                             label: "이메일",
                             placeholder: "you@example.com",
                             helper: "로그인에 사용할 이메일",
                             leadingSystemImage: "envelope")

                AppTextField(text: $filled,
                             label: "이메일 (입력됨)",
                             leadingSystemImage: "envelope")

                AppTextField(text: $errored,
                             label: "이메일 (에러)",
                             error: "올바른 이메일 형식이 아닙니다.",
                             leadingSystemImage: "envelope")

                AppTextField(text: $pw,
                             label: "비밀번호",
                             helper: "8자 이상",
                             isSecure: true,
                             leadingSystemImage: "lock")

                AppTextField(text: $disabled,
                             label: "비활성",
                             isEnabled: false,
                             leadingSystemImage: "person")
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
    }
}
