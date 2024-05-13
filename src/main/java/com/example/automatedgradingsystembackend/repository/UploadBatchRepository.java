package com.example.automatedgradingsystembackend.repository;

import com.example.automatedgradingsystembackend.domain.UploadBatchInfo;
import com.example.automatedgradingsystembackend.domain.UserInfo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UploadBatchRepository extends CrudRepository<UploadBatchInfo, Long> {
    public UploadBatchInfo save(UploadBatchInfo uploadBatchInfo);

    public UploadBatchInfo findUploadBatchInfoByBatchNumber(long batchNumber);

    List<UploadBatchInfo> findByUserInfo(UserInfo userInfo);

    UploadBatchInfo findByBatchNumber(long batchNumber);
}
