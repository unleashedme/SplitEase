package com.example.splitease.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Services throw java.nio.file.AccessDeniedException for ownership checks; without this it surfaces as a 500
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }

    // Spring's default error body hides the reason; the app shows it, e.g. why a custom split was rejected
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        String reason = e.getReason() == null ? "" : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", reason));
    }

    // Rejected by the multipart limits in application.properties before reaching the controller
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message", "Bill photo must be 5 MB or smaller"));
    }
}
