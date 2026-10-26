package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.DashboardSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static com.example.splitease.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ExpenseSplitRepo expenseSplitRepo;
    @Mock private GroupRepo groupRepository;
    @Mock private UserRepo userRepo;
    @Mock private GroupMemberRepo groupMemberRepo;
    @Mock private SettlementService settlementService;

    @InjectMocks
    private DashboardService dashboardService;

    private Users alice;
    private Groups trip;

    @BeforeEach
    void setUp() {
        alice = user("Alice", "alice@test.com");
        trip = group("Trip", alice);
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
    }

    @Test
    void userWithoutGroupsGetsZeroSummary() {
        when(groupMemberRepo.findByUser(alice)).thenReturn(List.of());

        DashboardSummaryDTO dto = dashboardService.getDashboardSummary("alice@test.com");

        assertThat(dto).isNotNull();
        assertThat(dto.totalUserExpense()).isEqualByComparingTo("0");
        assertThat(dto.amountUserOwes()).isEqualByComparingTo("0");
        assertThat(dto.amountOwedToUser()).isEqualByComparingTo("0");
    }

    @Test
    void owedAndOwingComeFromTheSimplifiedDebts() {
        when(groupMemberRepo.findByUser(alice)).thenReturn(List.of(member(trip, alice, "admin")));
        when(settlementService.debtTotals(alice))
                .thenReturn(new SettlementService.DebtTotals(new BigDecimal("20.00"), new BigDecimal("30.00")));
        ExpenseSplits hotel = new ExpenseSplits();
        hotel.setShareAmount(new BigDecimal("30.00"));
        ExpenseSplits food = new ExpenseSplits();
        food.setShareAmount(new BigDecimal("20.00"));
        when(expenseSplitRepo.findAllByUserEmail("alice@test.com")).thenReturn(List.of(hotel, food));

        DashboardSummaryDTO dto = dashboardService.getDashboardSummary("alice@test.com");

        assertThat(dto.amountUserOwes()).isEqualByComparingTo("20.00");
        assertThat(dto.amountOwedToUser()).isEqualByComparingTo("30.00");
        assertThat(dto.totalUserExpense()).isEqualByComparingTo("50.00");
    }

    @Test
    void ignoresNullShareAmountsInTotal() {
        when(groupMemberRepo.findByUser(alice)).thenReturn(List.of(member(trip, alice, "admin")));
        when(settlementService.debtTotals(alice))
                .thenReturn(new SettlementService.DebtTotals(BigDecimal.ZERO, BigDecimal.ZERO));
        ExpenseSplits broken = new ExpenseSplits();
        ExpenseSplits ok = new ExpenseSplits();
        ok.setShareAmount(new BigDecimal("12.50"));
        when(expenseSplitRepo.findAllByUserEmail("alice@test.com")).thenReturn(List.of(broken, ok));

        DashboardSummaryDTO dto = dashboardService.getDashboardSummary("alice@test.com");

        assertThat(dto.totalUserExpense()).isEqualByComparingTo("12.50");
        assertThat(dto.amountOwedToUser()).isEqualByComparingTo("0");
        assertThat(dto.amountUserOwes()).isEqualByComparingTo("0");
    }
}
