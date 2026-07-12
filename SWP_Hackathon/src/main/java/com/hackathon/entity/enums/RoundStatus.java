package com.hackathon.entity.enums;

public enum RoundStatus {
    UPCOMING,    // Vòng thi chưa diễn ra (Đang chờ)
    ONGOING,     // Vòng thi đang diễn ra (Thí sinh đang làm bài/nộp bài)
    EVALUATING,  // Vòng thi đã đóng nộp bài, Hội đồng đang tiến hành chấm điểm
    APPEALING, // Vòng thi bước vào giai đoạn phúc khảo
    COMPLETED,   // Vòng thi đã hoàn thành (Đã có kết quả, đã chốt điểm)

    DRAFT_APPROVED,
    FINAL_APPROVED,

    PENDING_APPROVAL,  // chờ duyệt sau khi hết thời gian chấm
    PENDING_FINAL_APPROVAL,// chờ duyệt sau khi kết thúc phúc khảo
    RE_EVALUATING,
//    APPROVED
}
