package com.hackathon.repository;

import com.hackathon.entity.TeamRequest;
import com.hackathon.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRequestRepository extends JpaRepository<TeamRequest, Integer> {

    //Tìm các request PENDING chưa có ai nhận (expertAssign IS NULL)
    //thuộc về các Đội thi đấu ở Hạng mục mà Mentor này được phân công.
    @Query("""
                SELECT tr
                FROM TeamRequest tr
                JOIN tr.team t
                JOIN t.registrations reg
                JOIN reg.participants p
                WHERE tr.status = :requestStatus
                  AND tr.expertAssign IS NULL
                  AND reg.status = :registrationStatus
                  AND p.categoryRound.round.status = :roundStatus
                  AND p.categoryRound.categoryRoundId IN (
                        SELECT ea.categoryRound.categoryRoundId
                        FROM ExpertAssign ea
                        WHERE ea.expert.expertId = :expertId
                          AND ea.role = :role
                  )
            """)
    List<TeamRequest> findRequestForExpertRoleMentor(
            @Param("expertId") Integer expertId,
            @Param("requestStatus") RequestStatus requestStatus,
            @Param("registrationStatus") RegistrationStatus registrationStatus,
            @Param("roundStatus") RoundStatus roundStatus,
            @Param("role") ExpertRole role
    );
    @Query ("SELECT COUNT(c) " +
            "FROM TeamRequest c " +
            "WHERE c.round.roundId =: roundId " +
            "AND c.status IN :statuses " +
            "AND c.requestType =: requestType")
    long countByRoundAndStatuses(
            @Param("roundId") Integer roundId,
            @Param("statuses") List<RequestStatus> statuses,
            @Param("requestType")RequestType requestType);

    boolean existsByTeam_TeamIdAndStatusAndRequestType(Integer teamId, RequestStatus status, RequestType type);

    List<TeamRequest> findByRound_RoundIdAndRequestType(Integer roundId, RequestType requestType);

    List<TeamRequest> findByRound_RoundId(Integer roundId);

    List<TeamRequest> findByRound_RoundIdAndRequestTypeAndStatus(Integer roundId, RequestType requestType, RequestStatus status);
//    List<TeamRequest> findByRound_RoundIdAndRequestTypeAndStatusIn(Integer roundId, RequestType type, List<RequestStatus> statuses);

}
