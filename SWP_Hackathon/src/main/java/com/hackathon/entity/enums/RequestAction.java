package com.hackathon.entity.enums;

public enum RequestAction {
    REJECT, //từ chối request
    REQUEST_RE_EVALUATION, //chuyển judge chấm lại
    UPDATE_DRAW_RESULT,//cập nhật bốc thăm
    RESOLVE //hoàn tất request
}
