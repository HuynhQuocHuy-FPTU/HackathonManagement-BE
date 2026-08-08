package com.hackathon.repository;

import com.hackathon.entity.TeamRequest;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.entity.enums.RequestStatus;
import com.hackathon.entity.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRequestRepository extends JpaRepository<TeamRequest, Integer> {

        // Tìm các request PENDING chưa có ai nhận (expertAssign IS NULL)
        // thuộc về các Đội thi đấu ở Hạng mục mà Mentor này được phân công.

        @Query(value = "SELECT tr FROM TeamRequest tr " +
                        "JOIN tr.team t " +
                        "JOIN t.registrations reg " +
                        "JOIN reg.participants p " +
                        "LEFT JOIN tr.expertAssign ea " +
                        "WHERE (" +
                        "   (tr.status IN ('RESOLVED', 'REJECTED') AND ea.expert.expertId = :expertId AND ea.role = 'MENTOR') "
                        +
                        "   OR (tr.status = 'PENDING' AND tr.expertAssign IS NULL)" +
                        ") " +
                        "AND reg.status = 'APPROVED' " +
                        "AND p.categoryRound.round.status = 'ONGOING' " +
                        "AND p.categoryRound.categoryRoundId IN " +
                        "    (SELECT assigned.categoryRound.categoryRoundId FROM ExpertAssign assigned " +
                        "     WHERE assigned.expert.expertId = :expertId " +
                        "     AND assigned.role = 'MENTOR')", countQuery = "SELECT COUNT(tr) FROM TeamRequest tr " +
                                        "JOIN tr.team t " +
                                        "JOIN t.registrations reg " +
                                        "JOIN reg.participants p " +
                                        "LEFT JOIN tr.expertAssign ea " +
                                        "WHERE (" +
                                        "   (tr.status IN ('RESOLVED', 'REJECTED') AND ea.expert.expertId = :expertId AND ea.role = 'MENTOR') "
                                        +
                                        "   OR (tr.status = 'PENDING' AND tr.expertAssign IS NULL)" +
                                        ") " +
                                        "AND reg.status = 'APPROVED' " +
                                        "AND p.categoryRound.round.status = 'ONGOING' " +
                                        "AND p.categoryRound.categoryRoundId IN " +
                                        "    (SELECT assigned.categoryRound.categoryRoundId FROM ExpertAssign assigned "
                                        +
                                        "     WHERE assigned.expert.expertId = :expertId " +
                                        "     AND assigned.role = 'MENTOR')")
        List<TeamRequest> findRequestForExpertRoleMentor(
                        @Param("expertId") Integer expertID);

        boolean existsByTeam_TeamIdAndStatusAndRequestType(Integer teamId, RequestStatus status, RequestType type);

        boolean existsByTeam_TeamIdAndRound_RoundIdAndStatusInAndRequestType(
                        Integer teamId,
                        Integer roundId,
                        List<RequestStatus> statuses,
                        RequestType type);

        List<TeamRequest> findByRound_RoundIdAndRequestType(Integer roundId, RequestType requestType);

        List<TeamRequest> findByRound_RoundId(Integer roundId);

        List<TeamRequest> findByRound_RoundIdAndRequestTypeAndStatus(Integer roundId, RequestType requestType,
                        RequestStatus status);

        Optional<TeamRequest> findByTeam_TeamIdAndRound_RoundIdAndRequestTypeAndStatus(
                        Integer teamId,
                        Integer roundId,
                        RequestType requestType,
                        RequestStatus status);

        // List<TeamRequest> findByRound_RoundIdAndRequestTypeAndStatusIn(Integer
        // roundId, RequestType type, List<RequestStatus> statuses);

        List<TeamRequest> findByRound_HackathonEvent_EventIdAndRequestTypeNotOrderByCreateDateDesc(
                        Integer eventId,
                        RequestType excludedRequestType);

        long countByRound_RoundIdAndRequestTypeAndStatusAndExpertAssign_Expert_ExpertId(
                        Integer roundId,
                        RequestType requestType,
                        RequestStatus status,
                        Integer expertId);

        @Query("SELECT DISTINCT tq FROM TeamRequest tq " +
                        "JOIN tq.round r " +
                        "JOIN tq.team t " +
                        "JOIN t.teamMembers tm " +
                        "WHERE r.hackathonEvent.eventId = :eventId " +
                        "AND tq.requestType IN :requestTypes " +
                        "AND tm.student.studentId = :studentId " +
                        "ORDER BY tq.createDate DESC")
        List<TeamRequest> findMyRequestsByEventAndTypes(
                        @Param("eventId") Integer eventId,
                        @Param("requestTypes") List<RequestType> requestTypes,
                        @Param("studentId") Integer studentId);

        @Query("SELECT DISTINCT tq FROM TeamRequest tq " +
                        "JOIN tq.team t " +
                        "JOIN t.teamMembers tm " +
                        "WHERE tq.requestType IN :requestTypes " +
                        "AND tm.student.studentId = :studentId " +
                        "ORDER BY tq.createDate DESC")
        List<TeamRequest> findAllMyRequestsByTypes(
                        @Param("requestTypes") List<RequestType> requestTypes,
                        @Param("studentId") Integer studentId);
}
