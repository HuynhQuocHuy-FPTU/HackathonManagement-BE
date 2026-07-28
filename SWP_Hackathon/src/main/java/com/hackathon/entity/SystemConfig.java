package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "System_Config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = true)
    private String configKey; // Ví dụ: "MAX_TEAM_SIZE"

    @Column(nullable = true)
    private String configValue; // Ví dụ: "5"
}
