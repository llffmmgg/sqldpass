import SwiftUI

/// 객관식 4지선다 OMR 입력 (Inked OMR 디자인 시스템).
///
/// 각 보기는 `AppOptionRow` primitive 가 담당. `QuestionContentView` 가 보기 본문을
/// frozen 렌더로 그린다 — `.lineLimit` 절대 적용하지 않음.
struct OMRAnswerGrid: View {
    let question: MockExamQuestionItem
    let selectedOption: Int?
    let onSelect: (Int) -> Void
    /// 이미 선택된 보기를 다시 탭(= 두 번째 클릭)했을 때 호출. View 쪽에서 다음 문제로 넘김.
    var onAdvance: () -> Void = {}

    private let choices = [1, 2, 3, 4]

    var body: some View {
        let parsed = QuestionParser.parse(question.content)

        VStack(spacing: Spacing.sm) {
            ForEach(choices, id: \.self) { value in
                let text = parsed.options.indices.contains(value - 1)
                    ? parsed.options[value - 1]
                    : nil
                AppOptionRow(
                    optionNumber: value,
                    optionText: text,
                    state: selectedOption == value ? .selected : .idle,
                    onTap: {
                        // 이미 선택된 보기 두 번째 탭은 다음 문항 advance — light(네비게이션).
                        // 신규 선택은 selection(미세한 선택 변경).
                        if selectedOption == value {
                            Haptics.light()
                            onAdvance()
                        } else {
                            Haptics.selection()
                            onSelect(value)
                        }
                    }
                )
            }
        }
    }
}
