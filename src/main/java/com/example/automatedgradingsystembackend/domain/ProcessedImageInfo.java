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
public class ProcessedImageInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long processedImageInfoId;

    private String path;

    private BigDecimal score;

    private int answerNumber;
}