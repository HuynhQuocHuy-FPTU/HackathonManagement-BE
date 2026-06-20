package com.hackathon.repository;

import com.hackathon.entity.Round;
import com.hackathon.entity.enums.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findAllByHackathonEvent_EventId(Integer hackathonEventEventId);

    List<Round> findByStatus(RoundStatus status);

    List<Round> findByStatusNot(RoundStatus status);

    Optional<Round> findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(int hackathonEventEventId);
}
