package com.hackathon.repository;


import com.hackathon.entity.Registration;
import com.hackathon.entity.Student;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
=======
import org.springframework.stereotype.Repository;
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1

import java.util.List;
import java.util.Optional;
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

    // Check student nay co ton tai trong nhom nay khong
    boolean existsByTeamAndStudent(Team team, Student student);

    boolean existsByStudent(Student student);

    //kiểm tra xem Sinh viên đã nằm trong Team nào chưa
    List<TeamMember> findByStudent(Student student);


//     Tim Student thuoc Team nao
    Optional<TeamMember> findByTeamAndStudent(Team team, Student student);

    // Check Student co phai leader ko
//    Optional<TeamMember> findByStudentAndIsLeader(Student student, boolean isLeader);
    Optional<TeamMember> findByTeamAndIsLeader(Team team, boolean isLeader);
    List<TeamMember> findByTeam(Team team);


}