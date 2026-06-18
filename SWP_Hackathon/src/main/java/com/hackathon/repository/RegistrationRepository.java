package com.hackathon.repository;

import com.hackathon.entity.Registration;
import com.hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Integer> {
    List<Registration> findByTeam(Team team);

    Optional<Registration> findByTeamAndHackathonEvent_EventId(Team team ,Integer eventId);
//    boolean existsByHackathonEvent_EventIdAndTeam_TeamNameAndTeam_IdNot(Integer eventId, String teamName, Integer teamId);
}
