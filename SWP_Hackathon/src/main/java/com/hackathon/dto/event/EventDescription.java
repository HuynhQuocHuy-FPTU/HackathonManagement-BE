package com.hackathon.dto.event;

import java.util.List;

public record EventDescription(
        String introduction,

        List<Prize> prizes,

        List<String> participantBenefits,

        List<String> disqualificationRules,

        List<String> competitionRules

) {
}
