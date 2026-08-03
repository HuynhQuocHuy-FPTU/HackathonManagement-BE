package com.hackathon.repository;

import com.hackathon.entity.Round;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

@Component
public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findAllByHackathonEvent_EventId(Integer hackathonEventEventId);

    List<Round> findByStatusIn(List<RoundStatus> statuses);

    List<Round> findByStatusNot(RoundStatus status);

    Optional<Round> findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(int hackathonEventEventId);

    @Query("SELECT r FROM Round r JOIN r.hackathonEvent e " +
            "WHERE r.status NOT IN :excludedRoundStatuses " +
            "AND e.status NOT IN :allowedEventStatuses")
    List<Round> findRoundsOfActiveEvents(
            @Param("excludedRoundStatuses") List<RoundStatus> excludedRoundStatuses,
            @Param("allowedEventStatuses") List<EventStatus> allowedEventStatuses
    );

    List<Round> findByStatusAndAppealEndTimeBefore(RoundStatus status, LocalDateTime time);


    List<Round> findByEvaluationDeadlineLessThanEqualAndScoringProcessedAtIsNull(
            LocalDateTime evaluationDeadline
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Round r WHERE r.roundId = :roundId")
    Optional<Round> findByIdForAdvancement(@Param("roundId") Integer roundId);

    Optional<Round> findRoundByHackathonEvent_EventIdAndOrderIndex(Integer hackathonEventEventId, Integer orderIndex);

    // Tìm round có index lớn nhất
    @Query("""
                SELECT r
                FROM Round r
                WHERE r.hackathonEvent.eventId = :eventId
                  AND r.orderIndex = (
                      SELECT MAX(r2.orderIndex)
                      FROM Round r2
                      WHERE r2.hackathonEvent.eventId = :eventId
                  )
            """)
    Optional<Round> findFinalRoundByEventId(@Param("eventId") Integer eventId);


    List<Round> findByEndTimeLessThanEqualAndAdvancementProcessedAtIsNull(LocalDateTime currentTime
    );

}
