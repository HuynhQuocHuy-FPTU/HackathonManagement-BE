package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.Expert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Integer> {
    Optional<Expert> findByAccount_AccountId(Integer accountId);
}
