package com.example.automatedgradingsystembackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationEvents {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationEvents.class);

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent event) {
        logger.error("Authorization denied: " + event.toString());
    }
}