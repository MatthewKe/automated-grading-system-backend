package com.example.automatedgradingsystembackend.repository;

import com.example.automatedgradingsystembackend.domain.OriginalImageInfo;
import org.springframework.data.repository.CrudRepository;

public interface OriginalImageInfoRepository extends CrudRepository<OriginalImageInfo, Long> {
    public OriginalImageInfo save(OriginalImageInfo uploadBatchInfo);
}
