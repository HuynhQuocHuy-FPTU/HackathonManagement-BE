package com.hackathon.repository;

import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Integer> {
    void deleteHackathonEventByEventId(int eventId);

    List<HackathonEvent> findByStatus(EventStatus status);

    List<HackathonEvent> findByEventNameContainingIgnoreCase(String eventName);
}
