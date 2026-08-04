package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamDraft;
import com.hackathon.entity.TeamInvitation;
import com.hackathon.entity.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    boolean existsByTeamDraftAndAccount(TeamDraft teamDraft, Account account);

    boolean existsByTeamDraftAndEmail(TeamDraft teamDraft, String email);

    List<TeamInvitation> findByTeamDraftAndStatus(TeamDraft teamDraft, InvitationStatus status);

    List<TeamInvitation> findByTeamDraft(TeamDraft teamDraft);

    List<TeamInvitation> findByTeam(Team team);

    long countByTeamDraftAndStatus(TeamDraft teamDraft, InvitationStatus status);

    boolean existsByAccountAndStatusAndTeamDraftNot(Account account, InvitationStatus status, TeamDraft teamDraft);
}
