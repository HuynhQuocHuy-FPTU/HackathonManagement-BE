package com.hackathon.entity;

import com.hackathon.entity.enums.ExpertRole;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Expert_Assign")
public class ExpertAssign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Assign_ID")
    private int assignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false)
    private ExpertRole role;

    //1 expert - N expertAssgin
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Expert_ID", nullable = false)
    private Expert expert;

    //1 expertAssign - N evaluation
    @OneToMany(mappedBy = "expertAssign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evaluation> evaluations = new ArrayList<>();

    //1  Category_round - N expertAssign
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Category_Round_ID",nullable = false)
    private CategoryRound categoryRound;

}



