package com.hackathon.entity.enums;

public enum EvaluationStatus {
    NOT_GRADED,   // chưa chấm
    PARTIALLY_GRADED,
    GRADED,      // đã chấm
    RE_EVALUATION, // Ban giám khảo tiến hành chấm điểm lại khi có yêu cầu phúc khảo
    RE_EVALUATED // Ban giám khảo đã hoàn thành việc chấm điểm lại
}
