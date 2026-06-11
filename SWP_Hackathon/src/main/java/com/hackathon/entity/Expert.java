package com.hackathon.entity;

import com.hackathon.entity.enums.ExpertType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
@Data
@Entity
@Table(name = "Expert")
public class Expert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Expert_ID")
    private int expertId;
    @Column(name = "Expert_Name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String expertName;
    @Column(name = "Department", columnDefinition = "NVARCHAR(255)")
    private String department;
    @Column(name = "Type")
    @Enumerated(EnumType.STRING)
    private ExpertType type;
    @Column(name = "Work_Place", columnDefinition = "NVARCHAR(255)")
    private String workplace;


    //1 Account - 1 Expert
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="Account_ID", unique = true, nullable = false)
    private Account account;

//    //1 Organization - N Expert
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name ="organization_ID",nullable = false)
//    private Organization organization;

    //1 Expert- N expertAssign
    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpertAssign> expertAssigns = new ArrayList<>();
}
