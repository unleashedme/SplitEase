package com.example.splitease.controller;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

/** Standalone MockMvc helpers so controller tests don't need a database or security context. */
final class ControllerTestSupport {

    static final Principal ALICE = () -> "alice@test.com";

    private ControllerTestSupport() {}

    static MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller).build();
    }
}
