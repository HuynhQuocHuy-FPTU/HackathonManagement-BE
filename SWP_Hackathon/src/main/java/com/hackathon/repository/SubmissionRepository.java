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

    List<Submission> findByTeamParticipant_IdAndIsFinalTrue(Integer teamParticipantId);

    @Query("SELECT DISTINCT s FROM Submission s " +
            "JOIN TeamParticipant tp ON s.teamParticipant.id = tp.id " +
            "JOIN CategoryRound cr ON tp.categoryRound.categoryRoundId = cr.categoryRoundId " +
            "WHERE cr.round.roundId = :roundId " +
            "AND s.team.teamId IN :teamIds " +
            "ORDER BY s.createAt DESC")
    List<Submission> findSubmissionForStudent(@Param("roundId") Integer roundId,
                                              @Param("teamIds") List<Integer> teamIds);

    List<Submission> findAllByOrderByCreateAtDesc();

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
                  AND tp.status IN ('PASSED', 'FAILED', 'ACTIVE')
            """)
    Submission findFinalSubmission(
            @Param("categoryRoundId") Integer categoryRoundId,
            @Param("teamId") Integer teamId);

    @Query("SELECT s FROM Submission s " +
            "WHERE s.teamParticipant.categoryRound.categoryRoundId = :categoryRoundId " +
            "AND s.isFinal = true " +
            "ORDER BY s.createAt DESC")
    List<Submission> findFinalSubmissionsByCategoryRoundId(@Param("categoryRoundId") Integer categoryRoundId);

}
