package com.hackathon.repository;

import com.hackathon.entity.Notification;
import com.hackathon.entity.Team;
import com.hackathon.entity.enums.NotificationStatus;
import com.hackathon.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByTeam(Team team);
    List<Notification> findByTeamAndTypeAndStatus(Team team, NotificationType type, NotificationStatus status);
}
