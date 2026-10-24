package com.example.splitease.requestAndResponse;

import java.util.UUID;

public record GroupMemberDTO(
        UUID userId,
        String name,
        String email
) {}
