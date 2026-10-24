package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.RecordSettlementRequest;
import com.example.splitease.requestAndResponse.SettlementSummaryResponse;
import com.example.splitease.service.SettlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.example.splitease.controller.ControllerTestSupport.ALICE;
import static com.example.splitease.controller.ControllerTestSupport.mvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    @Mock private SettlementService settlementService;

    @InjectMocks private SettlementController settlementController;

    @Test
    void payablesAreListed() throws Exception {
        when(settlementService.getMyPayables("alice@test.com"))
                .thenReturn(List.of(new SettlementSummaryResponse("bob-id", "Bob", new BigDecimal("25.50"))));

        mvc(settlementController).perform(get("/settlements/payables").principal(ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].toUserId").value("bob-id"))
                .andExpect(jsonPath("$[0].toUserName").value("Bob"))
                .andExpect(jsonPath("$[0].amount").value(25.50));
    }

    @Test
    void recordSettlementDelegatesToService() throws Exception {
        UUID bobId = UUID.randomUUID();

        mvc(settlementController).perform(post("/settlements").principal(ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUserId\":\"" + bobId + "\",\"amount\":25.50,\"note\":\"cash\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<RecordSettlementRequest> captor = ArgumentCaptor.forClass(RecordSettlementRequest.class);
        verify(settlementService).recordSettlement(captor.capture(), eq("alice@test.com"));
        assertThat(captor.getValue().getToUserId()).isEqualTo(bobId);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("25.50");
        assertThat(captor.getValue().getNote()).isEqualTo("cash");
    }
}
