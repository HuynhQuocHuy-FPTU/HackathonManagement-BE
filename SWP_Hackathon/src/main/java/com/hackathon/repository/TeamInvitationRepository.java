package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamDraft;
import com.hackathon.entity.TeamInvitation;
import com.hackathon.entity.enums.InvitationStatus;
import com.hackathon.entity.enums.InvitationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    boolean existsByTeamDraftAndAccount(TeamDraft teamDraft, Account account);

    List<TeamInvitation> findByTeamDraftAndStatus(TeamDraft teamDraft, InvitationStatus status);

    List<TeamInvitation> findByTeamDraft(TeamDraft teamDraft);

    List<TeamInvitation> findByTeam(Team team);

    long countByTeamDraftAndStatus(TeamDraft teamDraft, InvitationStatus status);

    boolean existsByTeamAndAccountAndStatusAndType(Team team, Account account, InvitationStatus status, InvitationType type);

    List<TeamInvitation> findByTypeAndStatusAndTeam(InvitationType type, InvitationStatus status, Team team);

    List<TeamInvitation> findByAccountAndTypeOrderByCreatedAtDesc(
            Account account,
            InvitationType type
    );
    boolean existsByTeamAndAccountAndStatus(Team team, Account account, InvitationStatus status);

    boolean existsByTeamDraftAndEmail(TeamDraft teamDraft, String email);

    boolean existsByAccountAndStatusAndTeamDraftNot(Account account, InvitationStatus status, TeamDraft teamDraft);
    Optional<TeamInvitation> findByTeamDraftAndAccount(TeamDraft draft, Account account);

}
