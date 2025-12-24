package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.RecordSettlementRequest;
import com.example.splitease.requestAndResponse.SettlementSummaryResponse;
import com.example.splitease.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;


@RestController
@RequestMapping("/settlements")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @GetMapping("/payables")
    public List<SettlementSummaryResponse> getMyPayables(Principal principal){
        return settlementService.getMyPayables(principal.getName());
    }

    @PostMapping
    public ResponseEntity<?> recordSettlement(@RequestBody RecordSettlementRequest request, Principal principal){
        settlementService.recordSettlement(request, principal.getName());
        return ResponseEntity.ok().build();
    }
}
