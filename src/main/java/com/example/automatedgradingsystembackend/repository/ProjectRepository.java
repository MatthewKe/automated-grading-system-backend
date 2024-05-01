package com.example.automatedgradingsystembackend.repository;

import com.example.automatedgradingsystembackend.model.ProjectInfo;
import com.example.automatedgradingsystembackend.model.UserInfo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProjectRepository extends CrudRepository<ProjectInfo, Long> {
    public List<ProjectInfo> findByUserUsername(String username);

    public ProjectInfo save(ProjectInfo projectInfo);

    ProjectInfo findByProjectIdAndUserUsername(Long id, String username);

    ProjectInfo findByProjectId(Long id);

}
