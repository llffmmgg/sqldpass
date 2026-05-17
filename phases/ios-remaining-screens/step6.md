# Step 6 — 프로필 상세 + 메뉴 NavigationLink 연결 + 최종 검증

## Background

ProfileView 가 현재 정적 placeholder 메뉴만 있다. 본 step 에서:

1. ProfileView 재작성 — `MemberMe` 실데이터 표시 + 메뉴 NavigationLink (학습 기록/북마크/인사이트/프로필 편집/계정 삭제)
2. NicknameEditView 신규 — 닉네임 편집 (PATCH /api/members/me/nickname)
3. AccountDeletionConfirmView 신규 — 2단계 확인 (체크박스 + destructive 버튼) → DELETE /api/members/me → AuthStore.signOut → SessionGate 가 자동으로 AuthView 복귀
4. 최종 빌드 + 시뮬레이터 스크린샷 검증

## Workdir

```bash
ios/
```

## Dependencies

- Step 1: `MemberService.updateNickname`, `MemberService.deleteAccount` (이미 존재)
- Step 2: `HistoryView`
- Step 3: `WrongAnswersView` (재작성됨)
- Step 4: `BookmarksView`
- Step 5: `InsightsView`
- 기존: `AuthStore.signOut`, `MemberMe`, `MemberService.me`

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Profile/ProfileView.swift` | 재작성 — 실데이터 + 메뉴 NavigationLink |
| `ios/Sqldpass/Features/Profile/ProfileViewModel.swift` | 신규 — me 로딩 |
| `ios/Sqldpass/Features/Profile/NicknameEditView.swift` | 신규 — 닉네임 편집 화면 |
| `ios/Sqldpass/Features/Profile/AccountDeletionConfirmView.swift` | 신규 — 2단계 계정 삭제 |

## Implementation

### `ProfileViewModel.swift`

```swift
import Foundation
import Observation

@Observable
final class ProfileViewModel {
    private(set) var me: MemberMe?
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            me = try await MemberService.me()
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func updateLocalNickname(_ nickname: String) {
        guard let current = me else { return }
        me = MemberMe(
            id: current.id,
            nickname: nickname,
            provider: current.provider,
            createdAt: current.createdAt
        )
    }
}
```

### `ProfileView.swift` (재작성)

기존 ProfileView 의 `List` 안에 NavigationLink 가 없는 placeholder 메뉴들을 실제 화면으로 연결.

```swift
import SwiftUI

struct ProfileView: View {
    @State private var viewModel = ProfileViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    profileHeader
                }
                Section("학습") {
                    NavigationLink {
                        HistoryView()
                    } label: {
                        Label("학습 기록", systemImage: "list.bullet.rectangle.portrait")
                    }
                    NavigationLink {
                        BookmarksView()
                    } label: {
                        Label("북마크", systemImage: "bookmark")
                    }
                    NavigationLink {
                        InsightsView()
                    } label: {
                        Label("인사이트", systemImage: "chart.line.uptrend.xyaxis")
                    }
                }
                Section("구독") {
                    // 결제 화면은 Phase 4 별도 작업 — placeholder Label
                    Label("프리미엄 보기", systemImage: "crown")
                        .foregroundStyle(Color.appTextSubtle)
                }
                Section("계정") {
                    NavigationLink {
                        NicknameEditView(
                            current: viewModel.me?.nickname ?? "",
                            onUpdated: { viewModel.updateLocalNickname($0) }
                        )
                    } label: {
                        Label("프로필 편집", systemImage: "person.text.rectangle")
                    }
                    Button(role: .destructive) {
                        AuthStore.shared.signOut()
                    } label: {
                        Label("로그아웃", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                    NavigationLink {
                        AccountDeletionConfirmView()
                    } label: {
                        Label("계정 삭제", systemImage: "person.crop.circle.badge.minus")
                            .foregroundStyle(Color.semanticDanger)
                    }
                }
            }
            .navigationTitle("내정보")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.me == nil {
                    await viewModel.load()
                }
            }
        }
    }

    private var profileHeader: some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: "person.crop.circle.fill")
                .resizable()
                .frame(width: 56, height: 56)
                .foregroundStyle(Color.appTextSubtle)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(viewModel.me?.nickname ?? "로그인 상태 확인 중")
                    .font(AppType.bodyEmph)
                if let provider = viewModel.me?.provider {
                    Text("\(provider.lowercased()) 계정")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                } else if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                }
            }
        }
        .padding(.vertical, Spacing.xs)
    }
}

#Preview {
    ProfileView()
}
```

### `NicknameEditView.swift`

```swift
import SwiftUI

struct NicknameEditView: View {
    let current: String
    let onUpdated: (String) -> Void

    @State private var input: String = ""
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    @Environment(\.dismiss) private var dismiss
    @FocusState private var focused: Bool

    var body: some View {
        Form {
            Section {
                TextField("닉네임", text: $input)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .focused($focused)
                    .submitLabel(.done)
                    .onSubmit {
                        Task { await submit() }
                    }
            } footer: {
                Text("2~20자, 다른 사용자에게 보입니다.")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
            }
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                }
            }
        }
        .navigationTitle("프로필 편집")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task { await submit() }
                } label: {
                    if isSubmitting {
                        ProgressView()
                    } else {
                        Text("저장")
                            .fontWeight(.semibold)
                    }
                }
                .disabled(!isValid || isSubmitting)
            }
        }
        .onAppear {
            input = current
            focused = true
        }
    }

    private var isValid: Bool {
        let trimmed = input.trimmingCharacters(in: .whitespaces)
        return trimmed.count >= 2 && trimmed.count <= 20 && trimmed != current
    }

    private func submit() async {
        guard isValid else { return }
        isSubmitting = true
        errorMessage = nil
        let nickname = input.trimmingCharacters(in: .whitespaces)
        do {
            _ = try await MemberService.updateNickname(nickname)
            onUpdated(nickname)
            dismiss()
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isSubmitting = false
    }
}
```

### `AccountDeletionConfirmView.swift`

```swift
import SwiftUI

struct AccountDeletionConfirmView: View {
    @State private var checked = false
    @State private var isDeleting = false
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                header

                VStack(alignment: .leading, spacing: Spacing.sm) {
                    Text("삭제 시 즉시 적용됩니다:")
                        .font(AppType.bodyEmph)
                    BulletItem(text: "프로필, 풀이 기록, 오답노트, 북마크 모두 영구 삭제")
                    BulletItem(text: "구독 중이라도 환불은 자동 처리되지 않음 (별도 문의)")
                    BulletItem(text: "동일 소셜 계정으로 재가입은 가능, 데이터 복구 불가")
                }
                .padding(Spacing.base)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.appSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.lg)
                        .stroke(Color.appBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.lg))

                Toggle(isOn: $checked) {
                    Text("위 내용을 확인했습니다")
                        .font(AppType.body)
                }
                .tint(Color.semanticDanger)

                if let errorMessage {
                    Text(errorMessage)
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticDanger)
                }
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle("계정 삭제")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            Button(role: .destructive) {
                Task { await delete() }
            } label: {
                if isDeleting {
                    ProgressView().frame(maxWidth: .infinity).frame(height: 52)
                } else {
                    Text("영구 삭제하기")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.semanticDanger)
            .disabled(!checked || isDeleting)
            .padding(.horizontal, Spacing.base)
            .padding(.bottom, Spacing.sm)
            .background(Color.appPage)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.largeTitle)
                .foregroundStyle(Color.semanticDanger)
            Text("정말 계정을 삭제할까요?")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(Color.appTextPrimary)
            Text("이 작업은 되돌릴 수 없습니다.")
                .font(AppType.body)
                .foregroundStyle(Color.appTextMuted)
        }
    }

    private func delete() async {
        isDeleting = true
        errorMessage = nil
        do {
            try await MemberService.deleteAccount()
            AuthStore.shared.signOut()
            // SessionGate 가 isAuthenticated 변화 감지 후 AuthView 자동 전환
        } catch let error as APIError {
            errorMessage = error.errorDescription
            isDeleting = false
        } catch {
            errorMessage = error.localizedDescription
            isDeleting = false
        }
    }
}

private struct BulletItem: View {
    let text: String
    var body: some View {
        HStack(alignment: .top, spacing: Spacing.xs) {
            Text("•")
                .foregroundStyle(Color.appTextSubtle)
            Text(text)
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
```

## Validation

### 빌드 검증 (필수)

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대: `** BUILD SUCCEEDED **`

### 시뮬레이터 검증 (필수, 본 phase 통합 검증)

iPhone 15 Pro 시뮬레이터에 설치 + 4탭 + Profile 진입까지 스크린샷.

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl uninstall booted com.sqldpass.app 2>&1 | head -1
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 3
xcrun simctl io booted screenshot /tmp/ios-remaining-screens-final.png
```

실 데이터(API 응답) 검증은 백엔드 dev 서버 + 로그인 토큰 필요 — 본 phase 범위 외. 빌드 + 화면 진입 가능성만 확인.

## 금지사항

- 계정 삭제 후 `dismiss()` 호출 금지. 이유: AuthStore.signOut 으로 SessionGate 가 AuthView 로 자동 교체 — 별도 dismiss 불필요. 오히려 네비게이션 스택이 sign-out 직후 어색하게 튀어오를 수 있음.
- 로그아웃 버튼에 추가 확인 다이얼로그 금지. 이유: 토큰만 폐기되고 데이터 손실 없으니 즉시 처리. 계정 삭제와 구분.
- 닉네임 편집 화면을 sheet 가 아니라 `NavigationLink` 푸시로. 이유: Form 안에 시스템 키보드와 함께 표시할 때 navigation push 가 표준 UX.
- 결제(`프리미엄 보기`) 메뉴를 NavigationLink 로 만들지 마라. 이유: Phase 4 별도 작업 — 본 phase 에서는 placeholder Label 만.
