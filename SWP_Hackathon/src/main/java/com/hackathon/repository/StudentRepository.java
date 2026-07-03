package com.hackathon.repository;

import com.hackathon.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    boolean existsByStudentCode(String studentCode);
    Student findByStudentCode(String studentCode);

    @Query("SELECT s FROM Student  s " +
            "LEFT JOIN FETCH s.teamMembers " +
            "WHERE s.studentId =:studentId")
    Optional<Student> findByIdWithTeamMembers(@Param("studentId") Integer studentId);

}
