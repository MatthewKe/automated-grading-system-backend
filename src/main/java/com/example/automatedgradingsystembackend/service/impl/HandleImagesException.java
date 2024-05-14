package com.example.automatedgradingsystembackend.service.impl;

import lombok.Data;

@Data
public class HandleImagesException extends Exception {
    public HandleImagesException(String message) {
        super(message);
    }
}
