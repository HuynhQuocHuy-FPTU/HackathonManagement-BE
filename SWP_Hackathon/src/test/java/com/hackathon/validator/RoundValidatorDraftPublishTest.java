package com.hackathon.validator;

import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.UpdateTimeRoundRequest;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoundValidatorDraftPublishTest {

    private final RoundValidator validator = new RoundValidator();

    @Test
    void draftCreateAllowsMissingTimeline() {
        CreateRoundRequest request = new CreateRoundRequest();
        HackathonEvent event = new HackathonEvent();

        assertDoesNotThrow(
                () -> validator.validatorCreate(request, event)
        );
    }

    @Test
    void publishRejectsMissingRoundTimeline() {
        Round round = new Round();
        HackathonEvent event = new HackathonEvent();
        event.setStartDate(LocalDateTime.now().plusDays(1));
        event.setEndDate(LocalDateTime.now().plusDays(10));

        assertThrows(
                BadRequestException.class,
                () -> validator.validateRoundForPublish(round, event)
        );
    }

    @Test
    void updateTimeAfterPublishRejectsMissingTimeline() {
        Round round = new Round();
        round.setStatus(RoundStatus.UPCOMING);

        assertThrows(
                BadRequestException.class,
                () -> validator.validateTimeRound(
                        round,
                        new UpdateTimeRoundRequest()
                )
        );
    }
}
