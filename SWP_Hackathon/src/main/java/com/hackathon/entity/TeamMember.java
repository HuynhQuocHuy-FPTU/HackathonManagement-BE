package com.hackathon.entity;

import com.hackathon.entity.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "TeamMember")
public class TeamMember {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name ="Role",nullable = false)
    private Boolean isLeader;

    //1Student - N teamMember
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Student_ID", nullable = false)
    private Student student;

    //N TeamMember - 1 team
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Team_ID", nullable = false)
    private Team team;

}
