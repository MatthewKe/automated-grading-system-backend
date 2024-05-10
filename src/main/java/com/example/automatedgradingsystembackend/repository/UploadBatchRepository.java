package com.example.automatedgradingsystembackend.repository;

import com.example.automatedgradingsystembackend.domain.UploadBatchInfo;
import org.springframework.data.repository.CrudRepository;

public interface UploadBatchRepository extends CrudRepository<UploadBatchInfo, Long> {
    public UploadBatchInfo save(UploadBatchInfo uploadBatchInfo);
}
