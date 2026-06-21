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
@Table(name ="Category")
public class Category {
    @Id
    @GeneratedValue(strategy  = GenerationType.IDENTITY)
    @Column(name="Category_ID")
    private Integer categoryId;
    @Column(name = "Category_Name", columnDefinition = "NVARCHAR(255)", nullable = true)
    private String categoryName;

    //1 CATEGORY - N CATEGORY_ROUND
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<CategoryRound> categoryRounds = new ArrayList<>();

    // 1 HACKATHON - N CATEGORY
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Event_ID", nullable = false)
    private HackathonEvent hackathonEvent;


}
