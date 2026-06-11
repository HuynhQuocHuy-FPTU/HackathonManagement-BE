package com.hackathon.repository;

import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {
    Optional<TeamMember> findByTeamAndIsLeaderTrue(Team team);

}
