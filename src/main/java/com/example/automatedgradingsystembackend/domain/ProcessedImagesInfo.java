package com.example.automatedgradingsystembackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PROCESSED_IMAGES_IFNO")
@Builder
public class ProcessedImagesInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long processedImagesInfoId;

    private String path;

    @OneToOne(fetch = FetchType.EAGER)
    private ProjectInfo projectInfo;

    private BigDecimal score;

    @Column(scale = 2)
    private int answerNumber;
}