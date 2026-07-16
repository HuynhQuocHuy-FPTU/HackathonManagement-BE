package com.hackathon.repository;

import com.hackathon.entity.Submission;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.ExpertRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    @Query("SELECT DISTINCT s FROM Submission s " +
            "JOIN TeamParticipant tp ON s.teamParticipant.id = tp.id " +
            "JOIN CategoryRound cr ON tp.categoryRound.categoryRoundId = cr.categoryRoundId " +
            "WHERE cr.round.roundId = :roundId " +
            "AND s.team.teamId IN :teamIds")
    List<Submission> findSubmissionForStudent(@Param("roundId") Integer roundId,
                                              @Param("teamIds") List<Integer> teamIds);

    /**
     * Kéo toàn bộ danh sách Bài thi đã nộp bản cuối (isFinal = true) của một Vòng thi cụ thể.
     * Dùng JPQL để Join bắc cầu qua TeamParticipant.
     */

    @Query("""
                SELECT s
                FROM Submission s
                JOIN s.teamParticipant tp
                WHERE tp.categoryRound.categoryRoundId = :categoryRoundId
                  AND s.isFinal = true
                  AND s.team.teamId = :teamId
            """)
    Submission findFinalSubmission(
            @Param("categoryRoundId") Integer categoryRoundId,
            @Param("teamId") Integer teamId);

    @Query("SELECT s FROM Submission s " +
            "WHERE s.teamParticipant.categoryRound.categoryRoundId = :categoryRoundId " +
            "AND s.isFinal = true")
    List<Submission> findFinalSubmissionsByCategoryRoundId(@Param("categoryRoundId") Integer categoryRoundId);
//
//    @Query("""
//                SELECT COUNT(s)
//                FROM Submission s
//                WHERE s.teamParticipant.registration.hackathonEvent.eventId = :eventId
//                  AND EXISTS (
//                      SELECT ea
//                      FROM ExpertAssign ea
//                      WHERE ea.expert.expertId = :expertId
//                      AND ea.categoryRound = s.teamParticipant.categoryRound
//                      AND ea.role IN :roles
//                  )
//            """)
//    long countTotalAssigned(
//            @Param("expertId") Integer expertId,
//            @Param("eventId") Integer eventId,
//            @Param("roles") List<ExpertRole> roles
//    );
//
//    @Query("""
//                SELECT COUNT(s)
//                FROM Submission s
//                WHERE s.teamParticipant.categoryRound.round.hackathonEvent.eventId = :eventId
//                  AND EXISTS (
//                      SELECT ea
//                      FROM ExpertAssign ea
//                      WHERE ea.expert.expertId = :expertId
//                      AND ea.categoryRound = s.teamParticipant.categoryRound
//                  )
//                  AND NOT EXISTS (
//                      SELECT e
//                      FROM Evaluation e
//                      WHERE e.submission = s
//                      AND e.expertAssign.expert.expertId = :expertId
//                  )
//            """)
//    long countPendingReviews(@Param("expertId") Integer expertId,
//                             @Param("eventId") Integer eventId,
//                             @Param("statuses") List<EvaluationStatus> statuses,
//                             @Param("roles") List<ExpertRole> roles);


}
