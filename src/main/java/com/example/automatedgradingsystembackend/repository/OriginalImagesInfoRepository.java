package com.example.automatedgradingsystembackend.repository;

import com.example.automatedgradingsystembackend.domain.OriginalImagesInfo;
import org.springframework.data.repository.CrudRepository;

public interface OriginalImagesInfoRepository extends CrudRepository<OriginalImagesInfo, Long> {
    public OriginalImagesInfo save(OriginalImagesInfo uploadBatchInfo);
}
