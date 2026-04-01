package com.sqldpass.persistent.subject;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {

    List<SubjectEntity> findByParentIsNullOrderByDisplayOrder();

    List<SubjectEntity> findByParentIdOrderByDisplayOrder(Long parentId);
}
