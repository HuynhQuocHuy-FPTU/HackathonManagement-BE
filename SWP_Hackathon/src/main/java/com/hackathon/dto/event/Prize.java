package com.hackathon.dto.event;

import com.hackathon.entity.enums.PrizeType;

public record Prize(
        String title,
        String reward
) {

}
