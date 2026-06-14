package com.hackathon.repository;


import com.hackathon.entity.Registration;
import com.hackathon.entity.Student;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {
//    // Tim Student thuoc Team nao
    Optional<TeamMember> findByTeamAndStudent(Team team, Student student);

//    // Check student nay co ton tai trong nhom nay khong
    boolean existsByTeamAndStudent(Team team, Student student);

    // Check Student co phai leader ko
    Optional<TeamMember> findByStudentAndIsLeader(Student student, boolean isLeader);
    // Check leader co thuoc Team do ko
    Optional <TeamMember> findByTeamAndIsLeader(Team team , boolean isLeader);

    boolean existsByStudent(Student student);

    int countByTeam (Team team);

    Optional<TeamMember> findByStudent(Student team);




}