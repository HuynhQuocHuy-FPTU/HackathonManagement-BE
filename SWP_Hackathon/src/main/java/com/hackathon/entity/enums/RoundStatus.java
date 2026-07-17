package com.hackathon.entity.enums;

public enum RoundStatus {
    UPCOMING,    // Vòng thi chưa diễn ra (Đang chờ)
    ONGOING,     // Vòng thi đang diễn ra (Thí sinh đang làm bài/nộp bài)
    EVALUATING,  // Vòng thi đã đóng nộp bài, Hội đồng đang tiến hành chấm điểm
    COMPLETED,   // Vòng thi đã hoàn thành (Đã có kết quả, đã chốt điểm)
    APPEALING, // Vòng thi bước vào giai đoạn phúc khảo
    PENDING,
    FINAL_RESULT
}
