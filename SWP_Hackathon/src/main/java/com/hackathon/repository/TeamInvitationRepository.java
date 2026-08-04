package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamDraft;
import com.hackathon.entity.TeamInvitation;
import com.hackathon.entity.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    boolean existsByTeamDraftAndAccount(TeamDraft teamDraft, Account account);

    List<TeamInvitation> findByTeamDraftAndStatus(TeamDraft teamDraft, InvitationStatus status);

    List<TeamInvitation> findByTeamDraft(TeamDraft teamDraft);

    List<TeamInvitation> findByTeam(Team team);
    long countByTeamDraftAndStatus(TeamDraft teamDraft, InvitationStatus status);
//    @Modifying
//    @Query("UPDATE TeamInvitation t SET t.account = :account WHERE t.teamInvitationId = :id")
//    void updateAccountForInvitation(@Param("id") Long invitationId, @Param("account") Account account);
}
