package com.sqldpass.service.wronganswer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.controller.wronganswer.dto.WrongAnswerResponse;
import com.sqldpass.controller.wronganswer.dto.WrongAnswerStatsResponse;
import com.sqldpass.persistent.solve.SolveAnswerRepository;

@Service
@Transactional(readOnly = true)
public class WrongAnswerService {

    private final SolveAnswerRepository solveAnswerRepository;

    public WrongAnswerService(SolveAnswerRepository solveAnswerRepository) {
        this.solveAnswerRepository = solveAnswerRepository;
    }

    public List<WrongAnswerResponse> getWrongAnswers(Long memberId, Long subjectId) {
        return solveAnswerRepository.findWrongAnswers(memberId, subjectId).stream()
                .map(WrongAnswerResponse::from)
                .toList();
    }

    public List<WrongAnswerStatsResponse> getStats(Long memberId) {
        return solveAnswerRepository.findWrongAnswerStats(memberId).stream()
                .map(WrongAnswerStatsResponse::from)
                .toList();
    }
}
