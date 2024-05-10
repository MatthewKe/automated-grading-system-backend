package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.repository.UserInfo;
import com.example.automatedgradingsystembackend.repository.UserRole;

import java.util.Set;

public interface UserService {
    UserInfo updatePassword(Long userId, String newPassword);

    UserInfo updateRoles(Long userId, Set<UserRole> newRoles);

    boolean registerUser(UserInfo userInfo);
}
