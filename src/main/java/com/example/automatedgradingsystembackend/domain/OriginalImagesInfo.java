package com.example.automatedgradingsystembackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "ORIGINAL_IMAGES_INFO")
@Builder
public class OriginalImagesInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long originalImagesInfoId;

    private String path;

    private String studentId;

    private String studentName;

    private boolean successfulProcess;

    private String failedReason;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<ProcessedImagesInfo> processedImagesInfos = new HashSet<>();
}
