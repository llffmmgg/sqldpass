package com.sqldpass.controller.solve;

import java.util.List;

public record SolveRequest(Long subjectId, List<SolveAnswerRequest> answers) {
}
