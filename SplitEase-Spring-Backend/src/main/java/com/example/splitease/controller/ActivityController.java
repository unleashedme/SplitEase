package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.ActivityDashboardResponse;
import com.example.splitease.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.util.UUID;

@RestController
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/activity")
    public ResponseEntity<ActivityDashboardResponse> getActivity(Principal principal) {

        return ResponseEntity.ok(activityService.getUserActivity(principal.getName()));

    }
}
