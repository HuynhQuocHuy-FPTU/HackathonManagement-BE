package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamInvitation;
import com.hackathon.entity.enums.InvitationStatus;
import com.hackathon.entity.enums.NotiResponseStatus;
import com.hackathon.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTeam(Team team);

    List<Notification> findByTeamAndType(Team team, NotificationType type);

    List<Notification> findByAccount_AccountIdOrderByCreatedAtDesc(Integer accountId);

    List<Notification> findByAccount_AccountIdAndIsReadFalseOrderByCreatedAtDesc(Integer accountId);

    List<Notification> findByAccount_AccountIdAndIsReadTrueOrderByCreatedAtDesc(Integer accountId);

    List<Notification> findByAccount_AccountIdAndTypeOrderByCreatedAtDesc(Integer accountId, NotificationType type);

    Optional<Notification>
    findFirstByAccount_AccountIdAndRound_RoundIdAndTypeOrderByCreatedAtAsc(
            Integer accountId,
            Integer roundId,
            NotificationType type
    );

    List<Notification> findNotificationByAccount_AccountIdAndResponseStatus(int accountAccountId, NotiResponseStatus responseStatus);

    long countByAccount_AccountIdAndIsReadFalse(Integer accountId);

    void deleteByAccount_AccountId(Integer accountId);

    void deleteByAccount_AccountIdAndType(Integer accountId, NotificationType type);

    void deleteByTeam(Team team);

    List<Notification> findByTeamAndTypeAndStatus(Team team, NotificationType type, InvitationStatus status);

    List<Notification> findByAccountAndTypeAndStatus(Account account, NotificationType type, InvitationStatus status);

    Optional<Notification> findByTeamInvitation(TeamInvitation teamInvitation);
}
