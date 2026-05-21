import SwiftUI

/// 모의고사 풀이 화면 문제 카드 (Inked OMR 디자인 시스템 chrome).
///
/// 본문 렌더링은 frozen 한 `QuestionContentView` 가 그대로 담당한다.
/// `parsed.body` 가 있으면 본문만 분리 표시, 없으면 원본 content 그대로 표시.
/// 과목명/문제 번호 라벨은 상위 화면(`SolveView`) 의 meta strip 이 chrome 에 그리므로
/// 본문 카드에서는 중복 노출하지 않는다 (871lJPyM 디자인 정합).
struct QuestionBody: View {
    let question: MockExamQuestionItem

    var body: some View {
        let parsed = QuestionParser.parseNormalized(question.content)
        let bodyText = parsed.options.isEmpty || parsed.body.isEmpty
            ? question.content
            : parsed.body

        AppCard(surface: .card) {
            QuestionContentView(text: bodyText)
                .textSelection(.enabled)
        }
    }
}
