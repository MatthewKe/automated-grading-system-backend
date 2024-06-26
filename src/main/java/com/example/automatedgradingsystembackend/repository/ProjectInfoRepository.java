package com.example.automatedgradingsystembackend.repository;

import com.example.automatedgradingsystembackend.domain.ProjectInfo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProjectInfoRepository extends CrudRepository<ProjectInfo, Long> {
    public List<ProjectInfo> findByUserUsername(String username);

    public ProjectInfo save(ProjectInfo projectInfo);

    ProjectInfo findByProjectIdAndUserUsername(Long id, String username);

    ProjectInfo findByProjectId(Long id);

}
