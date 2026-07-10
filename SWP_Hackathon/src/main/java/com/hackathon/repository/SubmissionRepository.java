package com.hackathon.repository;

import com.hackathon.entity.Submission;
import com.hackathon.entity.enums.ExpertRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    @Query("SELECT DISTINCT s FROM Submission s " +
            "JOIN TeamParticipant  tp ON s.teamParticipant = tp " +
            "JOIN CategoryRound cr ON tp.categoryRound = cr " +
            "WHERE cr.round.roundId =:roundId " +
            "AND s.team.teamId =:teamId")
    List<Submission> findSubmissionForLeader(@Param("roundId") Integer roundId,
                                           @Param("teamId") Integer teamId);

    /**
     * Kéo toàn bộ danh sách Bài thi đã nộp bản cuối (isFinal = true) của một Vòng thi cụ thể.
     * Dùng JPQL để Join bắc cầu qua TeamParticipant.
     */
    @Query("SELECT s FROM Submission s " +
            "WHERE s.teamParticipant.categoryRound.categoryRoundId = :categoryRoundId " +
            "AND s.isFinal = true")
    List<Submission> findFinalSubmissionsByCategoryRoundId(@Param("categoryRoundId") Integer categoryRoundId);

}
