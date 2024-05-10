package com.example.automatedgradingsystembackend.service;

import com.example.automatedgradingsystembackend.repository.UserInfo;
import com.example.automatedgradingsystembackend.repository.UserRole;
import com.example.automatedgradingsystembackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);


    @Override
    public UserInfo updatePassword(Long userId, String newPassword) {
        logger.info("updatePassword begins");
        return userRepository.findById(userId).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id " + userId));
    }

    @Override
    public UserInfo updateRoles(Long userId, Set<UserRole> newRoles) {
        logger.info("updateRoles begins");
        return userRepository.findById(userId).map(user -> {
            user.setRoles(newRoles);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id " + userId));
    }

    @Override
    public boolean registerUser(UserInfo userInfo) {
        logger.info("registerUser begins");
        UserInfo userInfoExisted = userRepository.findByUsername(userInfo.getUsername());
        if (userInfoExisted != null) {
            logger.info("user existed");
            return false;
        } else {
            logger.info("userInfo save begins");
            userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
            userRepository.save(userInfo);
            logger.info("userInfo save succeed");
            return true;
        }
    }
}
