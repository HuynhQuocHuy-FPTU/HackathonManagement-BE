package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.TeamDraft;
import com.hackathon.entity.TeamInvitation;
import com.hackathon.entity.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamDraftRepository extends JpaRepository<TeamDraft, Long> {
    //    Optional<TeamDraft> findByLeaderAccount(Account account);
    Optional<TeamDraft> findByAccount(Account account);

    Optional<TeamDraft> findByTeamNameIgnoreCase(String teamName);

    List<TeamDraft> findAllByStatus(TeamStatus status);

    boolean existsByTeamNameIgnoreCase(String teamName);

    Optional<TeamDraft> findByAccount_AccountIdAndStatus(Integer accountId, TeamStatus status);}
