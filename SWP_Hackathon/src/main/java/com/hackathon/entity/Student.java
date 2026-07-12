package com.hackathon.entity;

import com.hackathon.entity.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name="Student")
public class Student {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name="Student_ID")
    private int studentId;
    @Column(name = "Student_Code",unique = true, columnDefinition = "VARCHAR(20)" ,nullable = false)
    private String studentCode;
    @Column(name = "Student_Name", columnDefinition = "NVARCHAR(50)" ,nullable = false)
    private String studentName;
    @Column(name = "Address", columnDefinition = "NVARCHAR(255)")
    private String address;
    @Column(name = "University_Name" , nullable = true,columnDefinition = "NVARCHAR(255)")
    private  String universityName;
    @Column(name = "Major", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String major;

    //1 account - 1 student
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="Account_ID", unique = true)
    private Account account;

    // 1 Student - N Team
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<TeamMember> teamMembers = new ArrayList<>();



}
