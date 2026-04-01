package com.sqldpass.service.subject;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.subject.Subject;
import com.sqldpass.persistent.subject.SubjectEntity;
import com.sqldpass.persistent.subject.SubjectRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class SubjectServiceTest {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void setUp() {
        subjectRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        SubjectEntity subject1 = new SubjectEntity(null, "1과목: 데이터 모델링의 이해", 1);
        subjectRepository.save(subject1);

        SubjectEntity child1 = new SubjectEntity(subject1, "데이터 모델링의 이해", 1);
        SubjectEntity child2 = new SubjectEntity(subject1, "데이터 모델과 SQL", 2);
        subjectRepository.save(child1);
        subjectRepository.save(child2);

        SubjectEntity subject2 = new SubjectEntity(null, "2과목: SQL 기본 및 활용", 2);
        subjectRepository.save(subject2);

        SubjectEntity child3 = new SubjectEntity(subject2, "SQL 기본", 1);
        subjectRepository.save(child3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("과목 트리를 조회하면 루트 과목과 하위 과목이 트리 구조로 반환된다")
    void getSubjectTree() {
        List<Subject> tree = subjectService.getSubjectTree();

        assertThat(tree).hasSize(2);

        Subject first = tree.get(0);
        assertThat(first.getName()).isEqualTo("1과목: 데이터 모델링의 이해");
        assertThat(first.getChildren()).hasSize(2);
        assertThat(first.getChildren().get(0).getName()).isEqualTo("데이터 모델링의 이해");
        assertThat(first.getChildren().get(1).getName()).isEqualTo("데이터 모델과 SQL");

        Subject second = tree.get(1);
        assertThat(second.getName()).isEqualTo("2과목: SQL 기본 및 활용");
        assertThat(second.getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("과목이 없으면 빈 리스트를 반환한다")
    void getSubjectTree_empty() {
        subjectRepository.deleteAll();

        List<Subject> tree = subjectService.getSubjectTree();

        assertThat(tree).isEmpty();
    }
}
