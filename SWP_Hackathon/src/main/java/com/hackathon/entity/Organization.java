package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
@Data
@Entity
@Table(name="Organization")
public class Organization {
    @Id
    @Column(name="Organization_ID", nullable = false, columnDefinition = "VARCHAR(50)")
    private String organizationID;
    @Column(name="Organization_Name", nullable = false, columnDefinition = "NVARCHAR(255)", unique = true)
    private String organizationName;
    @Column(name="Address", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String address;
    @Column(name = "Short_Name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String shortName;
    @Column(name = "Email_Domain", nullable = false, columnDefinition = "NVARCHAR(255)", unique = true)
    private String emailDomain;
    @Enumerated(EnumType.STRING)
    @Column(name = "Type", nullable = false)
    private OrganizationType type;

    //1 Organization - N student
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore
    private List<Student> students = new ArrayList<>();

    //1 Organization - N Expert
    @OneToMany(mappedBy = "organization",cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Expert> experts = new ArrayList<>();

    //1 Organization - N Event Coordinator
    @OneToMany(mappedBy = "organization",cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<EventCoordinator> eventCoordinators = new ArrayList<>();



}
