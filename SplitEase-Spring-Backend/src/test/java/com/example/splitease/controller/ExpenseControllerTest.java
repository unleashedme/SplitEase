package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.AddExpenseRequest;
import com.example.splitease.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.util.UUID;

import static com.example.splitease.controller.ControllerTestSupport.ALICE;
import static com.example.splitease.controller.ControllerTestSupport.mvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock private ExpenseService expenseService;

    @InjectMocks private ExpenseController expenseController;

    @Test
    void addExpenseDelegatesToServiceWithPrincipal() throws Exception {
        UUID groupId = UUID.randomUUID();

        mvc(expenseController).perform(post("/expense").principal(ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\",\"amount\":120.50,\"description\":\"Fuel\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AddExpenseRequest> captor = ArgumentCaptor.forClass(AddExpenseRequest.class);
        verify(expenseService).addExpense(captor.capture(), eq("alice@test.com"));
        assertThat(captor.getValue().getGroupId()).isEqualTo(groupId);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("120.50");
        assertThat(captor.getValue().getDescription()).isEqualTo("Fuel");
    }

    @Test
    void deleteExpenseDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();

        mvc(expenseController).perform(delete("/expense/{id}", id).principal(ALICE))
                .andExpect(status().isOk());

        verify(expenseService).deleteExpense(id, "alice@test.com");
    }
}
