-- 카테고리 조회 시 subject(parent_id, display_order) 정렬 + WHERE 가속.
-- 적용 대상 쿼리: SubjectRepository.findByParentIdOrderByDisplayOrder, findRootsWithChildren의 자식 join.
-- 부수 효과: SubjectEntity.parent_id 컬럼의 단일 컬럼 인덱스 역할도 겸함.
CREATE INDEX idx_subject_parent_id ON subject(parent_id, display_order);
