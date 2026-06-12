package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "CriteriaDetail")
public class CriteriaDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Criteria_ID")
    private int criteriaId;
    @Column(name = "Criteria_Name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String criteriaName;
    @Column(name = "Weight", precision = 10, scale = 2, nullable = false)
    private BigDecimal weight;
    @Column(name = "Description",columnDefinition = "NVARCHAR(1000)")
    private String description;

    // 1 Criteria_set - N Criteria Detail
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CriteriaSet_Id", nullable = false)
    private CriteriaSet criteriaSet;


}
