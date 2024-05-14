package com.example.automatedgradingsystembackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "UPLOAD_BATCH_INFO")
@Builder
public class UploadBatchInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long batchNumber;

    @Column(name = "timestamp", columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.EAGER)
    private ProjectInfo projectInfo;

    private int numOfSuccess;
    private int numOfTotal;

    private String state;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<OriginalImageInfo> originalImageInfos = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private UserInfo userInfo;
}
