package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.Student;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {
    // Tim TeamMember cua account hien tai voi vai tro Leader
    Optional<TeamMember> findByStudent_AccountAndIsLeaderTrue(Account account);
    // Tim Student thuoc Team nao
    Optional<TeamMember> findByTeamAndStudent(Integer teamId, Student student);
    // Check student nay co ton tai trong nhom nay khong
    boolean existsByTeamAndStudent(Team team, Student student);
}
