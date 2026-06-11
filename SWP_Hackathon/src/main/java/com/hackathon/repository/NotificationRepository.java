package com.hackathon.repository;

import com.hackathon.entity.Notification;
import com.hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findAllByTeam(Team team);
}
