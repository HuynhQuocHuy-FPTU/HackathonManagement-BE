package com.hackathon.repository;

import com.hackathon.entity.Student;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {
    // lấy toàn bộ member của team
    List<TeamMember> findByTeam(Team team);

    // check leader
    Optional<TeamMember> findByTeamAndIsLeaderTrue(Team team);

    // check 1 user có trong team chưa
    boolean existsByTeamAndStudent(Team team, Student student);

    // đếm số member trong team (check max 3-5 người)
    int countByTeam(Team team);
}
