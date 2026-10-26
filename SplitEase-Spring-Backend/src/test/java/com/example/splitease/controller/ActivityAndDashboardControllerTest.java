package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.ActivityDashboardResponse;
import com.example.splitease.requestAndResponse.DashboardSummaryDTO;
import com.example.splitease.requestAndResponse.ExpenseActivityDTO;
import com.example.splitease.service.ActivityService;
import com.example.splitease.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.example.splitease.controller.ControllerTestSupport.ALICE;
import static com.example.splitease.controller.ControllerTestSupport.mvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ActivityAndDashboardControllerTest {

    @Mock private ActivityService activityService;
    @Mock private DashboardService dashboardService;

    @InjectMocks private ActivityController activityController;
    @InjectMocks private DashboardController dashboardController;

    @Test
    void activityReturnsDashboardForPrincipal() throws Exception {
        UUID id = UUID.randomUUID();
        when(activityService.getUserActivity("alice@test.com")).thenReturn(new ActivityDashboardResponse(
                1, new BigDecimal("50.00"), 1, 0,
                List.of(new ExpenseActivityDTO(id, "Dinner", "2026-01-01T00:00:00Z", true, "Trip",
                        new BigDecimal("100.00"), new BigDecimal("50.00"), false, null, true,
                        "Alice", "EQUAL", List.of()))));

        mvc(activityController).perform(get("/activity").principal(ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpensesCount").value(1))
                .andExpect(jsonPath("$.totalSpentCombined").value(50.00))
                .andExpect(jsonPath("$.expenseHistory[0].expenseId").value(id.toString()))
                .andExpect(jsonPath("$.expenseHistory[0].description").value("Dinner"))
                .andExpect(jsonPath("$.expenseHistory[0].groupName").value("Trip"))
                .andExpect(jsonPath("$.expenseHistory[0].hasReceipt").value(true));
    }

    @Test
    void dashboardReturnsSummaryForPrincipal() throws Exception {
        when(dashboardService.getDashboardSummary("alice@test.com")).thenReturn(new DashboardSummaryDTO(
                new BigDecimal("75.00"), new BigDecimal("20.00"), new BigDecimal("30.00")));

        mvc(dashboardController).perform(get("/dashboard").principal(ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUserExpense").value(75.00))
                .andExpect(jsonPath("$.amountUserOwes").value(20.00))
                .andExpect(jsonPath("$.amountOwedToUser").value(30.00));
    }
}
