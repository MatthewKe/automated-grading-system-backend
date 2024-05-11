package com.example.automatedgradingsystembackend.service.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
public class HandleImagesException extends Exception {
    public HandleImagesException(String message) {
        super(message);
    }
}
