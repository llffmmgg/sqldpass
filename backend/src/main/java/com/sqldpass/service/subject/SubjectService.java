package com.sqldpass.service.subject;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.subject.Subject;
import com.sqldpass.persistent.subject.SubjectMapper;
import com.sqldpass.persistent.subject.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getSubjectTree() {
        return subjectRepository.findByParentIsNullOrderByDisplayOrder().stream()
                .map(SubjectMapper::toDomain)
                .toList();
    }
}
