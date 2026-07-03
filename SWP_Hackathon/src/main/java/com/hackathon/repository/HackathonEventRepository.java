package com.hackathon.repository;

import com.hackathon.dto.event.EventResponse;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Integer> {
    @Modifying
    @Transactional
    @Query("DELETE FROM HackathonEvent e WHERE e.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Integer eventId);

    List<HackathonEvent> findByStatus(EventStatus status);

    List<HackathonEvent> findByEventNameContainingIgnoreCase(String eventName);

    boolean existsHackathonEventByEventName(String eventName);

    List<HackathonEvent> findHackathonEventByEventNameContainingIgnoreCaseAndStatus(String eventName, EventStatus status);

    @Query("SELECT e FROM HackathonEvent e WHERE e.status NOT IN :excludedStatuses")
    List<HackathonEvent> findAllActiveProcessingEvents(@Param("excludedStatuses") List<EventStatus> excludedStatuses);


}
