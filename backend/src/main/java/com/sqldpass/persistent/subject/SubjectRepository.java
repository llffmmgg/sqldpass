package com.sqldpass.persistent.subject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {

    List<SubjectEntity> findByParentIsNullOrderByDisplayOrder();

    List<SubjectEntity> findByParentIdOrderByDisplayOrder(Long parentId);

    List<SubjectEntity> findByChildrenIsEmpty();

    Optional<SubjectEntity> findByNameAndParentIsNull(String name);

    Optional<SubjectEntity> findByNameAndParentId(String name, Long parentId);
}
