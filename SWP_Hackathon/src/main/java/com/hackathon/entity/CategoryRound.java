package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "CategoryRound")
public class CategoryRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Category_Round_ID")
    private int categoryRoundId;

    // 1 category - N category_round
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Category_ID", nullable = false)
    private Category category;

    // 1 round - N category_round
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "Round_ID")
    private Round round;


    // 1 Category Round - N expertAssign
    @OneToMany(mappedBy = "categoryRound", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<ExpertAssign> expertAssigns = new ArrayList<>();

    //1 Category Round - N Participant
    @OneToMany(mappedBy = "categoryRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamParticipant> teamParticipants;

}
