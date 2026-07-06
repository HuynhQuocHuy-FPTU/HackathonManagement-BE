package com.hackathon.entity.enums;

public enum RoundStatus {
    UPCOMING,    // Vòng thi chưa diễn ra (Đang chờ)
    ONGOING,     // Vòng thi đang diễn ra (Thí sinh đang làm bài/nộp bài)
    EVALUATING,  // Vòng thi đã đóng nộp bài, Hội đồng đang tiến hành chấm điểm
    APPEALING, // Vòng thi bước vào giai đoạn phúc khảo
    COMPLETED,   // Vòng thi đã hoàn thành (Đã có kết quả, đã chốt điểm)
    APPROVED,   // Vòng thi đã được bản tổ chức phê duyệt kết quả chấm điểm
    PENDING_APPROVAL,
    RE_EVALUATING
}
