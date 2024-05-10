package com.example.automatedgradingsystembackend.repository;

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

    private String title;
    private int numOfSuccess;
    private int numOfTotal;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<OriginalImagesInfo> originalImagesInfos = new HashSet<>();
}
