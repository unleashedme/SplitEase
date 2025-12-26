package com.example.splitease.controller;


import com.example.splitease.requestAndResponse.DashboardSummaryDTO;
import com.example.splitease.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDTO> getSummaryStats(Principal principal){
        return ResponseEntity.ok(dashboardService.getDashboardSummary(principal.getName()));
    }
}
