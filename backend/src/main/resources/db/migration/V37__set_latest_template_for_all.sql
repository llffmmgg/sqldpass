-- 기존 모의고사 중 template이 null인 항목에 '최신 분포' 태그 일괄 적용
UPDATE mock_exam SET template = 'LATEST' WHERE template IS NULL;
