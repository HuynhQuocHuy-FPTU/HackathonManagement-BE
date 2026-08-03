package com.hackathon.repository;

import com.hackathon.dto.event.EventResponse;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM HackathonEvent e WHERE e.eventId = :eventId")
    Optional<HackathonEvent> findByIdForRegistrationApproval(@Param("eventId") Integer eventId);

    @Modifying
    @Transactional
    @Query("DELETE FROM HackathonEvent e WHERE e.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Integer eventId);

    List<HackathonEvent> findByStatusNotIn(List<EventStatus> status);

    List<HackathonEvent> findBySeasonYearAndStatusNotIn(Integer seasonYear, List<EventStatus> statuses);

    @Query("SELECT DISTINCT e.seasonYear FROM HackathonEvent e " +
            "WHERE e.seasonYear IS NOT NULL AND e.status NOT IN :excludedStatuses " +
            "ORDER BY e.seasonYear DESC")
    List<Integer> findDistinctSeasonYearsByStatusNotIn(
            @Param("excludedStatuses") List<EventStatus> excludedStatuses
    );

    long countByStatusNotIn(List<EventStatus> statuses);

    List<HackathonEvent> findByStatus(EventStatus status);

    List<HackathonEvent> findByStatusInAndRegistrationDeadlineLessThanEqual(
            List<EventStatus> statuses,
            LocalDateTime registrationDeadline
    );

    List<HackathonEvent> findBySeasonYear(Integer seasonYear);

    @Query("SELECT DISTINCT e.seasonYear FROM HackathonEvent e " +
            "WHERE e.seasonYear IS NOT NULL ORDER BY e.seasonYear DESC")
    List<Integer> findDistinctSeasonYears();

    List<HackathonEvent> findByEventNameContainingIgnoreCase(String eventName);

    boolean existsHackathonEventByEventName(String eventName);

    List<HackathonEvent> findHackathonEventByEventNameContainingIgnoreCaseAndStatus(String eventName, EventStatus status);

    @Query("SELECT e FROM HackathonEvent e WHERE e.status NOT IN :excludedStatuses")
    List<HackathonEvent> findAllActiveProcessingEvents(@Param("excludedStatuses") List<EventStatus> excludedStatuses);


}
