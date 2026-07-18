package com.hackathon.repository;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.ExpertRole;
import jdk.jfr.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertAssignRepository extends JpaRepository<ExpertAssign, Integer> {
    public List<ExpertAssign> findByCategoryRound_Round_RoundId(int roundId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ExpertAssign ea WHERE ea.categoryRound.round.hackathonEvent.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT ex FROM ExpertAssign ex " +
            "JOIN ex.categoryRound cr " +
            "JOIN cr.round r " +
            "WHERE ex.expert.expertId = :expertId " +
            "AND r.hackathonEvent.eventId = :eventId")
    List<ExpertAssign> findExpertAssignments(@Param("expertId") Integer expertId, @Param("eventId") Integer eventId);



    @Query("SELECT DISTINCT e FROM HackathonEvent e " +
            "JOIN Round c ON c.hackathonEvent = e " +
            "JOIN CategoryRound  cr ON cr.round = c " +
            "JOIN ExpertAssign ex ON ex.categoryRound = cr " +
            "WHERE ex.expert.expertId =:expertId " +
            "AND ex.role IN :roles")
    List<HackathonEvent> findEventByJudge(@Param("expertId") Integer expertId,
                                          @Param("roles")List<ExpertRole> expertRoles);

    @Query("SELECT DISTINCT cr.round FROM CategoryRound cr " +
            "JOIN ExpertAssign ex ON ex.categoryRound = cr " +
            "WHERE cr.round.hackathonEvent.eventId=:eventId " +
            "AND ex.expert.expertId =:expertId " +
            "AND ex.role IN :roles")
    List<Round> findRoundByJudge(@Param("eventId") Integer eventId,
                                 @Param("expertId") Integer expertId,
                                 @Param("roles")List<ExpertRole> expertRoles);

    @Query("SELECT DISTINCT cr FROM CategoryRound cr " +
            "JOIN ExpertAssign ex ON ex.categoryRound = cr " +
            "WHERE cr.round.roundId =:roundId " +
            "AND ex.expert.expertId =:expertId " +
            "AND ex.role IN :roles")
    List<CategoryRound> findCategoryByJudge(@Param("roundId") Integer eventId,
                                             @Param("expertId") Integer expertId,
                                             @Param("roles")List<ExpertRole> expertRoles);


    @Query("SELECT DISTINCT s FROM Submission s " +
            "JOIN TeamParticipant  tp ON s.teamParticipant = tp " +
            "JOIN CategoryRound cr ON tp.categoryRound = cr " +
            "JOIN ExpertAssign ex ON ex.categoryRound = cr " +
            "WHERE cr.categoryRoundId =:categoryRoundId " +
            "AND ex.expert.expertId =:expertId " +
            "AND ex.role IN :roles " +
            "AND s.isFinal = true")
    List<Submission> findSubmissionByJudge(@Param("categoryRoundId") Integer categoryRoundId,
                                             @Param("expertId") Integer expertId,
                                             @Param("roles")List<ExpertRole> expertRoles);



    @Query("SELECT ex FROM ExpertAssign ex " +
            "WHERE ex.categoryRound.categoryRoundId = :categoryRoundId")
    List<ExpertAssign> findByCategoryRoundId(@Param("categoryRoundId") Integer categoryRoundId);

    @Query("SELECT ex FROM ExpertAssign ex " +
            "JOIN ex.categoryRound cr " +
            "JOIN cr.round r " +
            "WHERE ex.expert.expertId = :expertId " +
            "AND ex.role = :role " +
            "AND r.hackathonEvent.eventId = :eventId")
    List<ExpertAssign> findExpertAssignmentsByRole(@Param("expertId") Integer expertId,
                                                   @Param("role") ExpertRole role,
                                                   @Param("eventId") Integer eventId);
    @Query("SELECT ex FROM ExpertAssign ex " +
            "WHERE ex.categoryRound.categoryRoundId = :categoryRoundId " +
            "AND ex.expert.expertId = :expertId " +
            "AND ex.role = 'MENTOR'")
    Optional<ExpertAssign> findMentorByExpertIdAndCategoryRoundId(
            @Param("categoryRoundId") Integer categoryRoundId,
            @Param("expertId") Integer expertId);

    @Query("SELECT COUNT(DISTINCT ea.expert.expertId) FROM ExpertAssign ea WHERE ea.role = :role")
    long countDistinctExpertByRole(@Param("role") ExpertRole role);

    /**
     * Trinh sát xem Chuyên gia này có thực sự là Giám khảo (Judge) của Vòng này không.
     * Dùng mệnh đề IN (:roles) để quét cùng lúc cả CORE_JUDGE và GUEST_JUDGE.
     */
    @Query("SELECT ea FROM ExpertAssign ea " +
            "WHERE ea.categoryRound.categoryRoundId = :categoryRoundId " +
            "AND ea.expert.expertId = :expertId " +
            "AND ea.role IN :roles")
    Optional<ExpertAssign> findJudgeAssignment(
            @Param("categoryRoundId") Integer categoryRoundId,
            @Param("expertId") Integer expertId,
            @Param("roles") List<ExpertRole> roles);

    /**
     * Dò xem Chuyên gia này có đang bị dính role MENTOR ở Vòng này không.
     * Tách riêng hàm này ra để Tầng Support bắt chính xác lỗi "Mentor cấm chấm điểm".
     */
    @Query("SELECT ea FROM ExpertAssign ea " +
            "WHERE ea.categoryRound.categoryRoundId = :categoryRoundId " +
            "AND ea.expert.expertId = :expertId " +
            "AND ea.role = 'MENTOR'")
    Optional<ExpertAssign> findMentorAssignment(
            @Param("categoryRoundId") Integer categoryRoundId,
            @Param("expertId") Integer expertId);

//    @Query("SELECT ex FROM ExpertAssign ex " +
//            "JOIN ex.categoryRound cr " +
//            "JOIN cr.round r " +
//            "WHERE ex.expert.expertId = :expertId " +
//            "AND ex.role IN :role" )
//    List<ExpertAssign> findExpertAssignmentsByRoleIn(@Param("expertId") Integer expertId, @Param("role") List<ExpertRole> role);



}
