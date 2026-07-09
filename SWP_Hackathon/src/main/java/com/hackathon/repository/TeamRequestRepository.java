package com.hackathon.repository;

import com.hackathon.entity.TeamRequest;
import com.hackathon.entity.enums.RequestStatus;
import com.hackathon.entity.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRequestRepository extends JpaRepository<TeamRequest,Integer> {

    //Tìm các request PENDING chưa có ai nhận (expertAssign IS NULL)
    //thuộc về các Đội thi đấu ở Hạng mục mà Mentor này được phân công.
    @Query("SELECT tr FROM TeamRequest tr " +
            "JOIN tr.team t " +
            "JOIN t.registrations reg " +
            "JOIN reg.participants p " +
            "WHERE tr.status = 'PENDING' " +
            "AND tr.expertAssign IS NULL " +  // Chỉ lấy request chưa ai nhận
            "AND reg.status = 'APPROVED' " +
            "AND p.categoryRound.round.status = 'ONGOING' " +
            "AND p.categoryRound.categoryRoundId IN " +
            "    (SELECT ea.categoryRound.categoryRoundId FROM ExpertAssign ea " +
            "     WHERE ea.expert.expertId = :expertId " +
            "     AND ea.role = 'MENTOR')")
    List<TeamRequest> findRequestForExpertRoleMentor(@Param("expertId") Integer expertID);

    boolean existsByTeam_TeamIdAndStatusAndRequestType(Integer teamId, RequestStatus status, RequestType type);

    List<TeamRequest> findByRound_RoundIdAndRequestType(Integer roundId, RequestType requestType);

    List<TeamRequest> findByRound_RoundIdAndRequestTypeAndStatus(Integer roundId, RequestType requestType, RequestStatus status);
    List<TeamRequest> findByRound_RoundIdAndRequestTypeAndStatusIn(Integer roundId, RequestType type, List<RequestStatus> statuses);

}
