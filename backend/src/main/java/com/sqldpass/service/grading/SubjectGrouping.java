package com.sqldpass.service.grading;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.subject.SubjectEntity;

/**
 * 자격증별 subject 트리 구조가 일관되지 않아, 합격 기준 과목 단위 표시는 ExamType 별 분기 필요.
 *
 * 시드 트리 비교:
 * - SQLD (V2): root 가 합격 단위 ("1과목/2과목"), 그 아래 leaf 카테고리 ("SQL 기본" 등)
 *              → 합격 단위는 leaf.parent
 * - 정처기 필기 / 컴활 / ADsP / 정처기 실기: root 가 자격증명 ("정보처리기사 필기" 등),
 *              그 아래가 leaf 합격 단위 (5과목/3과목/2과목)
 *              → 합격 단위는 leaf 자체
 *
 * 이 차이를 한곳에서 통일해 처리한다. 모의고사·기출 응시 화면, 채점 결과 모두 이 헬퍼를 사용.
 */
public final class SubjectGrouping {

    private SubjectGrouping() {
    }

    /**
     * 합격 기준 과목 단위 SubjectEntity 반환.
     * leaf 또는 leaf.parent. 잘못된 트리/null 도 안전 처리.
     */
    public static SubjectEntity groupOf(SubjectEntity leaf, ExamType examType) {
        if (leaf == null) return null;
        if (examType == ExamType.SQLD) {
            return leaf.getParent() != null ? leaf.getParent() : leaf;
        }
        // 그 외: leaf 자체가 합격 단위 (자격증 root 의 직속 자식)
        return leaf;
    }

    /**
     * 채점 결과 그룹핑용. 응시 화면(groupOf)과 다른 자격증이 두 개 있다:
     * - 정처기 실기: 단일 통합 과목 ("정보처리 실무"). 응시 화면에선 카테고리(C/Java/Python/SQL...)
     *   를 학습용으로 보여주지만, 채점은 자격증 root 단위(=단일 그룹)로 합쳐서 1줄로만 표시.
     */
    public static SubjectEntity scoringGroupOf(SubjectEntity leaf, ExamType examType) {
        if (leaf == null) return null;
        if (examType == ExamType.ENGINEER_PRACTICAL) {
            return leaf.getParent() != null ? leaf.getParent() : leaf;
        }
        return groupOf(leaf, examType);
    }

    public static String groupName(SubjectEntity leaf, ExamType examType) {
        SubjectEntity g = groupOf(leaf, examType);
        return g != null ? g.getName() : "기타";
    }

    public static Long groupId(SubjectEntity leaf, ExamType examType) {
        SubjectEntity g = groupOf(leaf, examType);
        return g != null ? g.getId() : null;
    }
}
