package com.hackathon.entity;

import com.hackathon.entity.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@Builder
@Entity
@Table(name="Student")
public class Student {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name="Student_ID")
    private int student_id;
    @Column(name = "Student_Code", columnDefinition = "VARCHAR(20)" ,nullable = false)
    private String studentCode;
    @Column(name = "Student_Name", columnDefinition = "NVARCHAR(50)" ,nullable = false)
    private String student_Name;
    @Column(name = "Address", columnDefinition = "NVARCHAR(255)",nullable = false )
    private String address;
    @Column(name = "Major", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String major;
    @Column(name = "Start_Date", nullable = false )
    private LocalDateTime startDate;
    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StudentStatus status;
    //1 account - 1 student
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="Account_ID", unique = true)
    private Account account;

    //1 Organization - N Student
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="organization_ID",nullable = false )
    private Organization organization;

    // 1 Student - N Team
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<TeamMember> teamMembers = new ArrayList<>();



}
