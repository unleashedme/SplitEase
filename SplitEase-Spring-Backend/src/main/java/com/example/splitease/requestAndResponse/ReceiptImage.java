package com.example.splitease.requestAndResponse;

import java.time.Instant;

/** One stored version (full or thumbnail) of a bill photo, ready to send to the app. */
public record ReceiptImage(
        String contentType,
        byte[] bytes,
        Instant createdAt
) {}
