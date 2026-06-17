package com.hackathon.repository;


import com.hackathon.entity.Registration;
import com.hackathon.entity.Student;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

    // Check student nay co ton tai trong nhom nay khong
    boolean existsByTeamAndStudent(Team team, Student student);

    boolean existsByStudent(Student student);

    //kiểm tra xem Sinh viên đã nằm trong Team nào chưa
    List<TeamMember> findByStudent(Student student);


//     Tim Student thuoc Team nao
    Optional<TeamMember> findByTeamAndStudent(Team team, Student student);

    // Check Student co phai leader ko
    Optional<TeamMember> findByStudentAndIsLeader(Student student, boolean isLeader);

    // Check leader co thuoc Team do ko
//    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm " +
//            "JOIN tm.team t " +
//            "JOIN t.registrations r " +
//            "WHERE tm.student.studentId = :studentId AND r.hackathonEvent.eventId = :eventId")
//    boolean isStudentAlreadyInEvent(@Param("studentId") Integer studentId, @Param("eventId") Integer eventId);
    Optional<TeamMember> findByTeamAndIsLeader(Team team, boolean isLeader);


}