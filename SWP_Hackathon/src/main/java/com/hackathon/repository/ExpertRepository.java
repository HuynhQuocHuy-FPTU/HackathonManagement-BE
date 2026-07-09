package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.Expert;
import com.hackathon.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Integer> {
    Optional<Expert> findByAccount_AccountId(Integer accountId);

    /**
     * Kiểm tra xem Expert (Giám khảo/Mentor) có đang được phân công (ExpertAssign)
     * vào bất kỳ Sự kiện (HackathonEvent) nào đang diễn ra (ONGOING, ACTIVE) hay không.
     */
    @Query("SELECT COUNT(ea) > 0 FROM ExpertAssign ea " +
            "JOIN ea.categoryRound cr " +
            "JOIN cr.round r " +
            "JOIN r.hackathonEvent e " +
            "WHERE ea.expert.expertId = :expertId " +
            "AND e.status IN :statuses")
    boolean isAssignedToOngoingEvent(
            @Param("expertId") Integer expertId,
            @Param("statuses") List<EventStatus> statuses
    );
}
