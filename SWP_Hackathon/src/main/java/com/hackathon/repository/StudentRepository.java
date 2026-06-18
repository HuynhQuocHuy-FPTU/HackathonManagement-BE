package com.hackathon.repository;

import com.hackathon.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    boolean existsByStudentCode(String studentCode);
    Student findByStudentCode(String studentCode);

}
