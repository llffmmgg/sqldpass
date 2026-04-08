package com.sqldpass.service.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionRepository;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

/**
 * 어드민 LLM 검증용 문제 Markdown export 서비스.
 *
 * - 다운로드 즉시 자동 export 마크 (다음엔 신규만)
 * - force=true 면 이미 마크된 것도 포함 (검증 재수행용)
 * - 마크 리셋 별도 메서드
 *
 * 형식: YAML frontmatter + Markdown 본문 + 구분자(===)
 * LLM이 코드 블록을 이스케이프 없이 정확히 읽을 수 있도록 Markdown 선택.
 */
@Service
@RequiredArgsConstructor
public class QuestionExportService {

    private static final String ENGINEER_ROOT_NAME = "정보처리기사 실기";
    private static final String COMPUTER_LITERACY_ROOT_NAME = "컴퓨터활용능력 1급 필기";
    private static final List<String> SINGLE_ROOT_EXCLUSIONS = List.of(ENGINEER_ROOT_NAME, COMPUTER_LITERACY_ROOT_NAME);

    private static final String EXAM_TYPE_SQLD = "SQLD";
    private static final String EXAM_TYPE_ENGINEER = "ENGINEER_PRACTICAL";
    private static final String EXAM_TYPE_COMPUTER_LITERACY = "COMPUTER_LITERACY_1";

    private final QuestionRepository questionRepository;

    /**
     * 지정 examType의 문제를 Markdown 문자열로 빌드하고, 다운로드된 문제는 export 마크.
     *
     * @param examType "SQLD" or "ENGINEER_PRACTICAL"
     * @param force    true면 이미 마크된 것도 포함
     * @return 빌드된 Markdown 문자열
     */
    @Transactional
    public ExportResult export(String examType, boolean force) {
        List<QuestionEntity> questions = fetchByExamType(examType, !force);

        String markdown = buildMarkdown(examType, questions);

        // 마크 일괄 업데이트 (응답 직전, 같은 트랜잭션)
        if (!questions.isEmpty()) {
            List<Long> ids = questions.stream().map(QuestionEntity::getId).toList();
            questionRepository.markAsExported(ids, LocalDateTime.now());
        }

        return new ExportResult(markdown, questions.size());
    }

    /**
     * 지정 examType의 export 마크를 모두 NULL로 리셋.
     *
     * @return 리셋된 행 수
     */
    @Transactional
    public int resetMark(String examType) {
        return switch (examType) {
            case EXAM_TYPE_SQLD -> questionRepository.resetSqldExportMark(SINGLE_ROOT_EXCLUSIONS);
            case EXAM_TYPE_ENGINEER -> questionRepository.resetEngineerExportMark(ENGINEER_ROOT_NAME);
            case EXAM_TYPE_COMPUTER_LITERACY -> questionRepository.resetEngineerExportMark(COMPUTER_LITERACY_ROOT_NAME);
            default -> throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "알 수 없는 examType: " + examType);
        };
    }

    private List<QuestionEntity> fetchByExamType(String examType, boolean onlyUnexported) {
        return switch (examType) {
            case EXAM_TYPE_SQLD ->
                    questionRepository.findSqldForExport(SINGLE_ROOT_EXCLUSIONS, onlyUnexported);
            case EXAM_TYPE_ENGINEER ->
                    questionRepository.findEngineerForExport(ENGINEER_ROOT_NAME, onlyUnexported);
            case EXAM_TYPE_COMPUTER_LITERACY ->
                    questionRepository.findEngineerForExport(COMPUTER_LITERACY_ROOT_NAME, onlyUnexported);
            default -> throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "알 수 없는 examType: " + examType);
        };
    }

    // ----------------------------------------------------------
    // Markdown 빌더
    // ----------------------------------------------------------

    private String buildMarkdown(String examType, List<QuestionEntity> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(examType).append(" 문제 검증 export\n");
        sb.append("- 총 ").append(questions.size()).append("개 문제\n");
        sb.append("- 생성 시각: ").append(LocalDateTime.now()).append("\n");
        sb.append("- 각 문제는 `===` 구분자로 분리됩니다.\n\n");

        if (questions.isEmpty()) {
            sb.append("> ⚠️ 다운로드할 신규 문제가 없습니다. ");
            sb.append("이미 검증한 문제까지 포함하려면 \"전체 강제 다운로드\"를 사용하세요.\n");
            return sb.toString();
        }

        sb.append("===\n\n");
        for (QuestionEntity q : questions) {
            appendQuestion(sb, examType, q);
            sb.append("===\n\n");
        }
        return sb.toString();
    }

    private void appendQuestion(StringBuilder sb, String examType, QuestionEntity q) {
        SubjectEntity subject = q.getSubject();
        String subjectPath = buildSubjectPath(subject);

        // YAML frontmatter — id 명시 (사용자가 검증 후 어드민에서 ID로 검색)
        sb.append("---\n");
        sb.append("id: ").append(q.getId()).append("\n");
        sb.append("examType: ").append(examType).append("\n");
        sb.append("subjectId: ").append(subject.getId()).append("\n");
        sb.append("subject: ").append(escapeYaml(subjectPath)).append("\n");
        sb.append("type: ").append(q.getQuestionType().name()).append("\n");
        if (q.getDifficulty() != null) {
            sb.append("difficulty: ").append(q.getDifficulty()).append("\n");
        }
        if (q.getQuestionType() == QuestionType.MCQ && q.getCorrectOption() != null) {
            sb.append("correctOption: ").append(q.getCorrectOption()).append("\n");
        }
        if (q.getTopic() != null) {
            sb.append("topic: ").append(escapeYaml(q.getTopic())).append("\n");
        }
        if (q.getSummary() != null) {
            sb.append("summary: ").append(escapeYaml(q.getSummary())).append("\n");
        }
        if (q.getExportedAt() != null) {
            sb.append("previouslyExportedAt: ").append(q.getExportedAt()).append("\n");
        }
        sb.append("---\n\n");

        // 헤더에도 ID 노출 (사람이 한눈에)
        sb.append("# Question #").append(q.getId());
        if (q.getSummary() != null) {
            sb.append(" — ").append(q.getSummary());
        }
        sb.append("\n\n");

        // 본문 (객관식 보기는 본문 내에 markdown으로 들어있는 구조)
        sb.append("## 본문\n");
        sb.append(q.getContent() == null ? "" : q.getContent().trim()).append("\n\n");

        // 정답
        sb.append("## 정답\n");
        if (q.getQuestionType() == QuestionType.MCQ) {
            sb.append(q.getCorrectOption() == null ? "(없음)" : String.valueOf(q.getCorrectOption()));
        } else {
            sb.append(q.getAnswer() == null ? "(없음)" : q.getAnswer().trim());
        }
        sb.append("\n\n");

        // 해설
        if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
            sb.append("## 해설\n");
            sb.append(q.getExplanation().trim()).append("\n\n");
        }

        // 키워드/허용답안 (단답/서술형)
        if (q.getQuestionType() != QuestionType.MCQ
                && q.getKeywords() != null && !q.getKeywords().isBlank()) {
            sb.append("## 키워드 / 허용답안\n");
            sb.append("```json\n").append(q.getKeywords().trim()).append("\n```\n\n");
        }
    }

    private String buildSubjectPath(SubjectEntity subject) {
        if (subject.getParent() != null) {
            return subject.getParent().getName() + " > " + subject.getName();
        }
        return subject.getName();
    }

    private String escapeYaml(String value) {
        if (value == null) return "";
        // YAML 안전을 위해 따옴표로 감싸고 내부 따옴표 이스케이프
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public record ExportResult(String markdown, int count) {}
}
