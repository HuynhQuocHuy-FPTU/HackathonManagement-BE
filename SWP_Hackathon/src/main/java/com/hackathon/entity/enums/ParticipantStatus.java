package com.hackathon.entity.enums;

public enum ParticipantStatus {
    ACTIVE,        // Đang thi đấu bình thường
    RE_EVALUATING, // Bài của team đang được giám khảo chấm lại
    PASSED,        // Đã qua vòng (được đi tiếp)
    DISQUALIFIED,    // Đã bị loại
    WITHDRAWN,     // Đội tự ý rút lui khỏi cuộc thi
    FAILED,    // Team không qua vòng,

}
