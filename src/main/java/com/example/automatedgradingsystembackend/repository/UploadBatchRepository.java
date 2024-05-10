package com.example.automatedgradingsystembackend.repository;

import org.springframework.data.repository.CrudRepository;

public interface UploadBatchRepository extends CrudRepository<UploadBatchInfo, Long> {
    public UploadBatchInfo save(UploadBatchInfo uploadBatchInfo);
}
