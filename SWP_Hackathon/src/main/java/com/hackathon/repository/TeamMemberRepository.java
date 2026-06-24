package com.hackathon.repository;


import com.hackathon.entity.Student;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

    // Check student nay co ton tai trong nhom nay khong
    boolean existsByTeamAndStudent(Team team, Student student);

    boolean existsByStudent(Student student);

    //kiểm tra xem Sinh viên đã nằm trong Team nào chưa
    List<TeamMember> findByStudent(Student student);

    //Tìm danh sách member bằng studentId
    List<TeamMember> findByStudent_StudentId(int studentId);

    //Tim Student thuoc Team nao
    Optional<TeamMember> findByTeamAndStudent(Team team, Student student);

    // Check Student co phai leader ko
    Optional<TeamMember> findByTeamAndIsLeader(Team team, boolean isLeader);

    List<TeamMember> findByTeam(Team team);

    Optional<TeamMember> findByTeam_TeamIdAndStudent(Integer teamId, Student student);

    Optional<TeamMember> findByStudentAndIsLeader(Student student, boolean isLeader);

}