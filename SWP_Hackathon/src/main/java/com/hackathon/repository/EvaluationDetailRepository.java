package com.hackathon.repository;

import com.hackathon.dto.analytics.RawScoreDTO;
import com.hackathon.entity.EvaluationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationDetailRepository  extends JpaRepository<EvaluationDetail, Integer> {
    List<EvaluationDetail> findByEvaluation_EvaluationId(int evaluationEvaluationId);

    // 1. Dữ liệu của Toàn Sự Kiện (Event)
    @Query("""
        SELECT new com.hackathon.dto.analytics.RawScoreDTO(
            ev.eventId, r.roundId, c.categoryId, ex.expertId, t.teamId, ec.evaluationCriteriaId, ec.criteriaName, ed.score
        )
        FROM EvaluationDetail ed
        JOIN ed.evaluation e
        JOIN e.expertAssign ea
        JOIN ea.expert ex
        JOIN ea.categoryRound cr
        JOIN cr.round r
        JOIN r.hackathonEvent ev
        JOIN cr.category c
        JOIN e.submission s
        JOIN s.team t
        JOIN ed.evaluationCriteria ec
        WHERE ev.eventId = :eventId
        AND e.status IN ('GRADED', 'RE_EVALUATED')
    """)
    List<RawScoreDTO> fetchRawScoresByEventId(@Param("eventId") Integer eventId);

    // 2. Dữ liệu của Vòng Thi (Round)
    @Query("""
        SELECT new com.hackathon.dto.analytics.RawScoreDTO(
            ev.eventId, r.roundId, c.categoryId, ex.expertId, t.teamId, ec.evaluationCriteriaId, ec.criteriaName, ed.score
        )
        FROM EvaluationDetail ed
        JOIN ed.evaluation e
        JOIN e.expertAssign ea
        JOIN ea.expert ex
        JOIN ea.categoryRound cr
        JOIN cr.round r
        JOIN r.hackathonEvent ev
        JOIN cr.category c
        JOIN e.submission s
        JOIN s.team t
        JOIN ed.evaluationCriteria ec
        WHERE r.roundId = :roundId
        AND e.status IN ('GRADED', 'RE_EVALUATED')
    """)
    List<RawScoreDTO> fetchRawScoresByRoundId(@Param("roundId") Integer roundId);

    // 3. Dữ liệu của Bảng Đấu (CategoryRound)
    @Query("""
        SELECT new com.hackathon.dto.analytics.RawScoreDTO(
            ev.eventId, r.roundId, c.categoryId, ex.expertId, t.teamId, ec.evaluationCriteriaId, ec.criteriaName, ed.score
        )
        FROM EvaluationDetail ed
        JOIN ed.evaluation e
        JOIN e.expertAssign ea
        JOIN ea.expert ex
        JOIN ea.categoryRound cr
        JOIN cr.round r
        JOIN r.hackathonEvent ev
        JOIN cr.category c
        JOIN e.submission s
        JOIN s.team t
        JOIN ed.evaluationCriteria ec
        WHERE cr.categoryRoundId = :categoryRoundId
        AND e.status IN ('GRADED', 'RE_EVALUATED')
    """)
    List<RawScoreDTO> fetchRawScoresByCategoryRoundId(@Param("categoryRoundId") Integer categoryRoundId);

    // 4. Dữ liệu của 1 Bài Dự Thi (Submission)
    @Query("""
        SELECT new com.hackathon.dto.analytics.RawScoreDTO(
            ev.eventId, r.roundId, c.categoryId, ex.expertId, t.teamId, ec.evaluationCriteriaId, ec.criteriaName, ed.score
        )
        FROM EvaluationDetail ed
        JOIN ed.evaluation e
        JOIN e.expertAssign ea
        JOIN ea.expert ex
        JOIN ea.categoryRound cr
        JOIN cr.round r
        JOIN r.hackathonEvent ev
        JOIN cr.category c
        JOIN e.submission s
        JOIN s.team t
        JOIN ed.evaluationCriteria ec
        WHERE s.submissionId = :submissionId
        AND e.status IN ('GRADED', 'RE_EVALUATED')
    """)
    List<RawScoreDTO> fetchRawScoresBySubmissionId(@Param("submissionId") Integer submissionId);


}
