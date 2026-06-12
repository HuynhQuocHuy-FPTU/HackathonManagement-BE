package com.hackathon.repository;

import com.hackathon.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface RoundRepository extends JpaRepository<Round, Integer> {
    public void deleteRoundByHackathonEvent_EventId(int eventId);
}
