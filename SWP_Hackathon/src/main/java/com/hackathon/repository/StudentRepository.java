package com.hackathon.repository;

import com.hackathon.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

<<<<<<< HEAD
import java.util.Optional;

=======
@Repository
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1
public interface StudentRepository extends JpaRepository<Student, Integer> {
    boolean existsByStudentCode(String studentCode);
    Student findByStudentCode(String studentCode);

}
