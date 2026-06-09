package com.hackathon.repository;

import com.hackathon.entity.HackathonEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Integer> {
    List<HackathonEvent> findByEventNameContainingIgnoreCase(String eventName);
}
