package com.hackathon.repository;

import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Integer> {
    void deleteHackathonEventByEventId(int eventId);

    List<HackathonEvent> findByStatus(EventStatus status);

    List<HackathonEvent> findByEventNameContainingIgnoreCase(String eventName);
}
