# Step 2 — App* primitives 3종 신설 (AppDialog · AppBottomSheet · AppDropdown)

## 배경

현재 14개 App* primitive 가 있지만 모달 계층(AlertDialog 8회, ModalBottomSheet 1회, DropdownMenu 6회)이 Material3 직접 노출 중. Step 3 의 치환 작업이 본 step 의 신규 primitive 를 사용.

본 step 은 Step 1 과 병렬 가능 (다른 파일).

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/common/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `AppDialog.kt` (신규) | AlertDialog wrap. AppButton + LocalSqldpassPalette.card + SqldRadius.xl |
| `AppBottomSheet.kt` (신규) | ModalBottomSheet wrap. LocalSqldpassPalette.card + SqldRadius.xxl 상단 라운드 |
| `AppDropdown.kt` (신규) | DropdownMenu + AppDropdownItem. LocalSqldpassPalette.card + SqldRadius.md |

## API

### AppDialog

```kotlin
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    confirmLabel: String = "확인",
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = "취소",
    onDismissAction: (() -> Unit)? = null,
    destructive: Boolean = false,
    content: (@Composable () -> Unit)? = null,
)
```

내부:
- `androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss)` 사용 (Material3 AlertDialog 회피, 시각 chrome 직접 그림)
- Card 시각: `LocalSqldpassPalette.current.card` 배경 + `RoundedCornerShape(SqldRadius.xl)`
- title (titleMedium, textPrimary) + message (bodyMedium, textMuted) + content slot (custom body)
- confirm: AppButton(text=confirmLabel, variant = if (destructive) Destructive else Primary)
- dismiss: AppButton(text=dismissLabel, variant = Tertiary) — dismissLabel null 이면 안 그림
- 패딩 SqldSpacing.lg, 액션 spacedBy SqldSpacing.sm
- Preview 4개: title-only / confirm-dismiss / destructive / custom-content

### AppBottomSheet

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
)
```

내부:
- `androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, ...)` 사용. `ExperimentalMaterial3Api` 는 본 컴포넌트 내부에 격리 (다른 곳 노출 X)
- `containerColor = LocalSqldpassPalette.current.card`
- `dragHandle = if (showDragHandle) BottomSheetDefaults.DragHandle() else { {} }` — 별 phase 에서 자체 DragHandle 로 교체 가능
- `shape` = top-only `RoundedCornerShape(topStart = SqldRadius.xxl, topEnd = SqldRadius.xxl)`
- 내부 Column: navigationBarsPadding + horizontal SqldSpacing.lg + vertical SqldSpacing.base
- Preview 2개: drag handle / no handle

### AppDropdown + AppDropdownItem

```kotlin
@Composable
fun AppDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
)

@Composable
fun AppDropdownItem(
    label: String,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
)
```

내부:
- AppDropdown: `androidx.compose.material3.DropdownMenu(expanded, onDismissRequest = onDismiss)` 사용. containerColor=card, shape=md
- AppDropdownItem: `DropdownMenuItem(text = { Text(label, ...) }, leadingIcon = { ... }, onClick)` 색은 destructive 면 LocalSqldpassPalette.danger, 일반은 textPrimary
- Preview 1개: 3개 아이템 (일반 2 + destructive 1)

## Acceptance Criteria

1. 3개 파일 신설, 각 Preview 포함
2. 각 primitive 가 `LocalSqldpassPalette` + `SqldSpacing` + `SqldRadius` 만 의존 (MaterialTheme.colorScheme 직접 사용 0)
3. 기존 호출처는 본 step 에서 변경 X (Step 3 가 담당)
4. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

Preview 렌더 — Android Studio 또는 IDE 에서 각 컴포넌트 Preview 확인.

## 금지 사항

- 기존 AlertDialog/ModalBottomSheet/DropdownMenu 호출처를 본 step 에서 수정하지 마라. 이유: Step 3 가 담당. 본 step 은 primitive 신설만.
- AppDialog 가 Material3 AlertDialog 를 직접 wrap 하지 마라. 이유: 시각 chrome 을 직접 제어해야 진정한 visible identity 분리. `Dialog(onDismissRequest)` 사용 권장.
- ExperimentalMaterial3Api 어노테이션을 외부 시그니처에 노출하지 마라. 이유: 내부에 격리.
- 신규 primitive 가 LocalSqldpassPalette 외 색상 토큰을 직접 정의하지 마라. 이유: 토큰 단일 진실 원천 유지.

## Status 규칙

- 성공: step 2 `completed`.
- 실패: 3회 후 `error`.
