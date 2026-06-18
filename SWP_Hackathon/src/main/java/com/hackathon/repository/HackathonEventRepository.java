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
<<<<<<< HEAD
import java.util.Optional;

=======
@Repository
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1
public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Integer> {
    @Modifying
    @Transactional
    @Query("DELETE FROM HackathonEvent e WHERE e.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Integer eventId);

    List<HackathonEvent> findByStatus(EventStatus status);

    List<HackathonEvent> findByEventNameContainingIgnoreCase(String eventName);

    boolean existsHackathonEventByEventName(String eventName);

    List<HackathonEvent> findHackathonEventByEventNameContainingIgnoreCaseAndStatus(String eventName, EventStatus status);


}
