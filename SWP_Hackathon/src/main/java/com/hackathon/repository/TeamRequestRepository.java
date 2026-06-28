package com.hackathon.repository;

import com.hackathon.entity.TeamRequest;
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
            "JOIN t.registrations r " +
            "JOIN r.participant p " +
            "WHERE  tr.responseStatus = NotiResponseStatus.PENDING " +
            "AND tr.expertAssign IS NULL " +
            "AND r.status = RegistrationStatus.APPROVED " +
            "AND p.categoryRound.round.status = RoundStatus.ONGOING " +
            "AND p.categoryRound.categoryRoundId IN " +
            " (SELECT ea.categoryRound.categoryRoundId FROM ExpertAssign ea WHERE ea.expert.expertId = :expertId)")
    List<TeamRequest> findRequestForExpert(@Param("expertId") Integer expertID);
}
