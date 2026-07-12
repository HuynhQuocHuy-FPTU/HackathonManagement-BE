package com.hackathon.entity;

import com.hackathon.entity.enums.ExpertType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Expert")
public class Expert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Expert_ID")
    private int expertId;
    @Column(name = "Expert_Name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String expertName;
    @Column(name = "Department", columnDefinition = "NVARCHAR(255)")
    private String department;
    @Column(name = "Organization", columnDefinition = "NVARCHAR(255)")
    private String organization;


    //1 Account - 1 Expert
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="Account_ID", unique = true, nullable = false)
    private Account account;

    //1 Expert- N expertAssign
    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpertAssign> expertAssigns = new ArrayList<>();
}
