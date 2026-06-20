package com.hackathon.repository;

import com.hackathon.entity.Registration;
import com.hackathon.entity.Team;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.entity.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Integer> {
    List<Registration> findByTeam(Team team);

    Optional<Registration> findByTeamAndHackathonEvent_EventId(Team team ,Integer eventId);
    List<Registration>findByHackathonEvent_EventIdAndStatus(Integer eventId, RegistrationStatus status);
//    boolean existsByHackathonEvent_EventIdAndTeam_TeamNameAndTeam_IdNot(Integer eventId, String teamName, Integer teamId);

    Optional<Registration> findByRegistrationId(Integer registrationId);
}
