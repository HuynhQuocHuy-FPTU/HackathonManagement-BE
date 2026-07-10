package com.hackathon.validator;

import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisqualifyValidator {


    public List<TeamParticipant> validateTeamBelongsToEventAndApproved(List<TeamParticipant> teamParticipants, Integer teamId, Integer eventId) {

        // 1. Nếu không có participant nào -> team không tham gia event này
        if (teamParticipants == null || teamParticipants.isEmpty()) {
            throw new BadRequestException("Team " + teamId + " không tham gia event " + eventId);
        }

        // 2. Lấy Registration đại diện (toàn bộ participant của 1 team trong 1 event
        //thuộc về cùng 1 Registration)
        Registration registration = teamParticipants.get(0).getRegistration();
        if (registration == null) {
            throw new BadRequestException("Team " + teamId + " không có đăng ký hợp lệ trong event " + eventId);
        }
        // 3. Double-check registration thực sự thuộc đúng event được truyền vào
        if (registration.getHackathonEvent() == null
                || !eventId.equals(registration.getHackathonEvent().getEventId())) {
            throw new BadRequestException("Team " + teamId + " không thuộc event " + eventId);
        }
        // 4. Chỉ cho phép disqualify khi đăng ký đã được APPROVED
        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new BadRequestException(
                    "Team " + teamId + " chưa được duyệt (APPROVED) trong event " + eventId + ", không thể thực hiện disqualify");
        }
        // 5. Chặn disqualify trùng lặp — nếu participant đã ở trạng thái DISQUALIFIED rồi thì không cho loại tiếp
        boolean alreadyDisqualified = teamParticipants.stream()
                .anyMatch(p -> p.getStatus() == ParticipantStatus.DISQUALIFIED);
        if (alreadyDisqualified) {
            throw new BadRequestException(
                    "Team " + teamId + " đã bị loại (DISQUALIFIED) khỏi event " + eventId + " trước đó");
        }
        return teamParticipants;
    }
}