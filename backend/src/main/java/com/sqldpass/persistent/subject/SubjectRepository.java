package com.sqldpass.persistent.subject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {

    List<SubjectEntity> findByParentIsNullOrderByDisplayOrder();

    List<SubjectEntity> findByParentIdOrderByDisplayOrder(Long parentId);

    List<SubjectEntity> findByChildrenIsEmpty();

    Optional<SubjectEntity> findByNameAndParentIsNull(String name);

    Optional<SubjectEntity> findByNameAndParentId(String name, Long parentId);

    /**
     * 모든 루트 과목과 직속 자식들을 한 번의 쿼리로 로드.
     * 공개 카테고리 응답을 만들 때 root → children N+1을 제거한다.
     */
    @Query("""
            SELECT DISTINCT s FROM SubjectEntity s
            LEFT JOIN FETCH s.children c
            WHERE s.parent IS NULL
            ORDER BY s.displayOrder
            """)
    List<SubjectEntity> findRootsWithChildren();
}
