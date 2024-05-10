package com.example.automatedgradingsystembackend.repository;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PROJECTS")
@Builder
public class ProjectInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private long projectId;


    @ManyToOne(fetch = FetchType.EAGER)
    private UserInfo user;


}