package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
@Data
@Entity
@Table(name ="Category")
public class Category {
    @Id
    @GeneratedValue(strategy  = GenerationType.IDENTITY)
    @Column(name="Category_ID")
    private int categoryId;
    @Column(name = "Category_Name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String categoryName;

    //1 CATEGORY - N CATEGORY_ROUND
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<CategoryRound> categoryRounds;

    // 1 HACKATHON - N CATEGORY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Event_ID", nullable = false)
    private HackathonEvent hackathonEvent;

}
