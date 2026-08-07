package com.hackathon.entity.enums;

public enum TeamStatus {
    PENDING,// Đã nộp đơn và chờ phê duyệt
    DRAFT,//  TEAM  TẠM THỜI, CHƯA ĐỦ THÀNH VIÊN TỐI THIỂU ĐỂ TẠO THÀNH 1 TEAM
    BUSY, // Đã đăng ký và đang tham gia cuộc thi
    FINISHED,// Đã hoàn thành cuộc thi,
    OFFICIAL,
    ACTIVE // LÀ TEAM ĐỦ THÀNH VIÊN TỐI THIỂU
    , DELETED
}
