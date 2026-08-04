package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.TeamDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamDraftRepository extends JpaRepository<TeamDraft,Long> {
//    Optional<TeamDraft> findByLeaderAccount(Account account);
Optional<TeamDraft> findByAccount(Account account);
//<TeamDraft> findByAccount(Account account);

}
