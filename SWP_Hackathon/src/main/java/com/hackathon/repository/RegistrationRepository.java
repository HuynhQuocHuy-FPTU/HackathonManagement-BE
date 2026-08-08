package com.hackathon.repository;

import com.hackathon.dto.TeamSelectionDTO;
import com.hackathon.entity.Registration;
import com.hackathon.entity.Team;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.entity.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Integer> {
    List<Registration> findByTeam(Team team);

    List<Registration> findByTeam_TeamIdOrderByRegistrationDateDesc(Integer teamId);

    List<Registration> findByStatus(RegistrationStatus status);

    Optional<Registration> findByTeamAndHackathonEvent_EventId(Team team, Integer eventId);

    List<Registration> findByHackathonEvent_EventIdAndStatus(Integer eventId, RegistrationStatus status);

    long countByHackathonEvent_EventIdAndStatus(Integer eventId, RegistrationStatus status);

    @Query("""
            SELECT DISTINCT r.team
            FROM Registration r
            WHERE r.status = :registrationStatus
              AND r.hackathonEvent.status NOT IN :excludedEventStatuses
            """)
    List<Team> findDistinctTeamsByRegistrationStatusAndEventStatusNotIn(
            @Param("registrationStatus") RegistrationStatus registrationStatus,
            @Param("excludedEventStatuses") List<EventStatus> excludedEventStatuses
    );

    List<Registration> findRegistrationByHackathonEvent_EventIdAndStatusIn(int hackathonEventEventId, List<RegistrationStatus> status);

    Optional<Registration> findRegistrationByRegistrationIdAndHackathonEvent_EventId(int registrationId, int hackathonEventEventId);

    @Query("SELECT r FROM Registration  r " +
            "WHERE r.hackathonEvent.eventId=:eventId " +
            "AND r.team.teamId =:teamId " +
            "AND r.status = RegistrationStatus.APPROVED")
    Optional<Registration> findByEventIdAndTeamId(@Param("eventId") Integer eventId, @Param("teamId") Integer teamId);

    List<Registration> findByTeam_TeamIdAndStatus(int teamTeamId, RegistrationStatus status);

    @Query("SELECT COUNT (r) " +
            "FROM Registration r " +
            "WHERE r.status =:regisStatus" +
            " AND r.hackathonEvent.eventId=:eventId")
    Integer countRegistration(@Param("regisStatus") RegistrationStatus registrationStatus, @Param("eventId") Integer eventId);



}
