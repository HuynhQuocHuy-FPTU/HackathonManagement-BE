package com.hackathon.repository;

import com.hackathon.entity.EventCoordinator;
import com.hackathon.entity.Student;
import com.hackathon.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    boolean existsByStudentCode(String studentCode);
    Student findByStudentCode(String studentCode);

    @Query("SELECT s FROM Student  s " +
            "LEFT JOIN FETCH s.teamMembers " +
            "WHERE s.studentId =:studentId")
    Optional<Student> findByIdWithTeamMembers(@Param("studentId") Integer studentId);

    /**
     * Kiểm tra xem sinh viên có đang tham gia đội nào đang ở trạng thái thi đấu hay không.
     * Dùng JPQL JOIN qua các bảng: TeamParticipant -> Registration -> Team -> TeamMember.
     * Bắn tham số dạng List<Enum> giúp tái sử dụng code tốt hơn.
     */
    @Query("SELECT COUNT(tp) > 0 FROM TeamParticipant tp " +
            "JOIN tp.registration r " +
            "JOIN r.team t " +
            "JOIN t.teamMembers tm " +
            "WHERE tm.student.studentId = :studentId " +
            "AND tp.status IN :statuses")
    boolean isParticipatingInOngoingCompetition(
            @Param("studentId") Integer studentId,
            @Param("statuses") List<ParticipantStatus> statuses
    );
    Optional<Student> findByAccount_AccountId(Integer accountId);
    Optional<Student> findByAccount_Email(String email);


    @Query("SELECT COUNT(s) FROM Student s WHERE " +
            "LOWER(s.account.email) LIKE '%fpt.edu.vn' " +
            "OR LOWER(s.universityName) LIKE '%fpt%'")
    long countFptStudents();

}
