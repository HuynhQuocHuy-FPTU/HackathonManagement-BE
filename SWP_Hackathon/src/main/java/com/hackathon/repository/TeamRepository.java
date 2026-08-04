package com.hackathon.repository;

import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Team;
import com.hackathon.entity.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {
    boolean existsByTeamNameIgnoreCase(String teamName);
    Optional<Team>findByTeamNameIgnoreCase(String teamName);
    boolean existsByTeamNameIgnoreCaseAndTeamIdNot(String name, Integer teamId);

    //     Tìm những team mà expert được phân công quản lý
    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN t.registrations r " +
            "JOIN r.participants p " +
            "JOIN p.categoryRound cr " +
            "JOIN ExpertAssign ex ON ex.categoryRound = cr " +
            "WHERE ex.expert.expertId = :expertId")
    List<Team> findTeamsByExpertAssignment(@Param("expertId") Integer expertId);


    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN Registration r ON r.team = t " +
            "JOIN  r.participants p " +
            "JOIN CategoryRound cr ON p.categoryRound = cr " +
            "JOIN ExpertAssign ea ON ea.categoryRound = cr " +
            "WHERE t.teamId = :teamId AND ea.expert.expertId = :expertId")
    Optional<Team> findTeamByIdAndExpertAssignment(@Param("teamId") Integer teamId, @Param("expertId") Integer expertId);

    @Query("SELECT t FROM Team t " +
            "JOIN t.teamMembers tm " +
            "WHERE tm.student.studentId = :studentId " +
            "AND tm.isLeader = true " +
            "AND t.status = com.hackathon.entity.enums.TeamStatus.BUSY")
    Optional<Team> findActiveLeadingTeamByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN Registration r ON r.team = t " +
            "JOIN r.participants p " +
            "JOIN CategoryRound cr ON p.categoryRound = cr " +
            "WHERE cr.categoryRoundId IN :categoryRoundId")
    List<Team> findTeamsByCategoryRoundId(@Param("categoryRoundId") List<Integer> categoryId);

    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN t.registrations r " +          // Team -> Regis (để lọc Event)
            "JOIN r.participants p " +            // Regis -> Participant
            "JOIN p.categoryRound cr " +         // Participant -> CategoryRound (để lọc Hạng mục)
            "WHERE cr.categoryRoundId IN :categoryRoundIds " + // Lọc theo list ID truyền vào
            "AND r.hackathonEvent.eventId = :eventId")
        // Lọc theo Event ID
    List<Team> findTeamsByCategoryRoundIdsAndEventId(
            @Param("categoryRoundIds") List<Integer> categoryRoundIds,
            @Param("eventId") Integer eventId
    );

    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN t.registrations r " +
            "JOIN r.participants p " +
            "JOIN p.categoryRound cr " +
            "JOIN ExpertAssign ex ON ex.categoryRound = cr " +
            "WHERE ex.expert.expertId = :expertId AND r.hackathonEvent.eventId = :eventId")
    List<Team> findTeamsByExpertAssignmentAndEvent(@Param("expertId") Integer expertId, @Param("eventId") Integer eventId);

    @Query("SELECT t FROM Team t " +
            "JOIN t.teamMembers tm " +
            "WHERE tm.student.studentId = :studentId")
    List<Team> findByStudent(@Param("studentId") Integer studentId);

    @Query("""
                SELECT t
                FROM Team t
                JOIN t.teamMembers tm
                WHERE tm.student.studentId = :studentId
                  AND t.status = :status
            """)
    Team findCurrentTeamByStudent(
            @Param("studentId") Integer studentId,
            @Param("status") TeamStatus status);

    @Query("""
                SELECT t
                FROM Team t
                JOIN t.teamMembers tm
                WHERE tm.student.studentId = :studentId
                  AND t.status IN :status
            """)
    Team findCurrentTeamByStudentAndStatus(
            @Param("studentId") Integer studentId,
            @Param("status") List<TeamStatus> status);

    List<Team> findByStatusAndTeamSizeLessThanOrderByCreateAtDesc(
            TeamStatus status,
            Integer maxTeamSize
    );
}

