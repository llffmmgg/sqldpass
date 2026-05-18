import SwiftUI

/// 이어풀기 추천 카드 — 마지막 풀이의 자격증·모드 기반.
///
/// `lastCertLabel` 또는 `lastMode` 가 없으면 호출부에서 카드 자체를 노출하지 않는다.
/// 본 컴포넌트는 값이 모두 있는 케이스만 다룬다.
///
/// 단일 진실 원천: docs/MOBILE_UX_SPEC.md § 2.1.
/// Android 미러: mobile/app/src/main/java/com/sqldpass/app/ui/home/ContinueLastCard.kt.
///
/// 본 step 의 HomeView 는 아직 lastCert/lastMode 데이터를 통합하지 않아
/// 호출 자체를 생략한다. 후속 phase 에서 lastCert/lastMode 연동 후 enable.
enum LastSolveMode {
    case practice
    case mockExam
    case pastExam
}

struct ContinueLastCard: View {
    let lastCertLabel: String
    let lastMode: LastSolveMode
    let onClick: () -> Void

    private var modeLabel: String {
        switch lastMode {
        case .practice:
            return "어제 풀던 \(lastCertLabel) 랜덤 10문 이어가기"
        case .mockExam:
            return "어제 시작한 \(lastCertLabel) 모의고사 이어가기"
        case .pastExam:
            return "어제 시작한 \(lastCertLabel) 기출 이어가기"
        }
    }

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: Spacing.md) {
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text("이어풀기")
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.brandPrimary)
                    Text(modeLabel)
                        .font(AppType.bodyEmph)
                        .foregroundStyle(Color.appTextPrimary)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: Spacing.sm)
                Image(systemName: "arrow.right")
                    .font(.callout)
                    .foregroundStyle(Color.appTextMuted)
            }
            .padding(Spacing.base)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.appSurface)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.lg)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack(spacing: Spacing.md) {
        ContinueLastCard(
            lastCertLabel: "SQLD",
            lastMode: .practice,
            onClick: {}
        )
        ContinueLastCard(
            lastCertLabel: "정처기 필기",
            lastMode: .mockExam,
            onClick: {}
        )
        ContinueLastCard(
            lastCertLabel: "ADsP",
            lastMode: .pastExam,
            onClick: {}
        )
    }
    .padding()
    .background(Color.appPage)
}
