//package com.hackathon.entity;
//
//import com.hackathon.entity.enums.RequestStatus;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//
//@NoArgsConstructor
//@AllArgsConstructor
//@Getter
//@Setter
//@Entity
//@Table(name = "TeamRequest")
//public class TeamRequest {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int requestId;
//    private String request;
//    private LocalDateTime createDate;
//    private RequestStatus status;
//    private String track;
//    private String response;
//    // 1 TEAM - N REQUEST
//
//    // 1 EXPERT ASSIGN - N REQUEST
//
//
//}
////