package com.hackathon.dto.team;

import java.time.LocalDateTime;
import java.util.List;

public record TeamActiveResponse(
        Integer teamId,
        String teamName,
        String leaderName,
        List<String> memberNames,
        LocalDateTime createAt,
        Integer maxTeamSize
) {
}
