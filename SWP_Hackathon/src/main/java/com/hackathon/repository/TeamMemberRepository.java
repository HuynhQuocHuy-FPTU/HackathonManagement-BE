package com.hackathon.repository;

import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {
    Optional<TeamMember> findByTeamAndIsLeaderTrue(Team team);

}
