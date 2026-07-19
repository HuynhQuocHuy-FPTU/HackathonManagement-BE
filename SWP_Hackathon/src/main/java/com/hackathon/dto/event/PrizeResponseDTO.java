package com.hackathon.dto.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrizeResponseDTO {
    private Integer teamParticipantId;
    private String prizeReward; // phần thưởng gồm dì
    private String prizeTitle;// tên giải thưởng
    private String teamName;
    private Integer ranking;
    private Integer eventId;
    private String eventName;
    private Integer roundId;
    private String roundName;

}
