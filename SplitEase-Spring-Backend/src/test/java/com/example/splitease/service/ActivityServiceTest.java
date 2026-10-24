package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.ExpenseRepo;
import com.example.splitease.requestAndResponse.ActivityDashboardResponse;
import com.example.splitease.requestAndResponse.ExpenseActivityDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.example.splitease.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ExpenseRepo expenseRepo;

    @InjectMocks
    private ActivityService activityService;

    private Users alice;
    private Users bob;
    private Groups trip;

    @BeforeEach
    void setUp() {
        alice = user("Alice", "alice@test.com");
        bob = user("Bob", "bob@test.com");
        trip = group("Trip", alice);
    }

    @Test
    void returnsEmptyDashboardWhenNoActivity() {
        when(expenseRepo.findAllActivityForUser("alice@test.com")).thenReturn(List.of());

        ActivityDashboardResponse res = activityService.getUserActivity("alice@test.com");

        assertThat(res.totalExpensesCount()).isZero();
        assertThat(res.totalSpentCombined()).isEqualByComparingTo("0");
        assertThat(res.expensesPaidByUser()).isZero();
        assertThat(res.settledExpensesCount()).isZero();
        assertThat(res.expenseHistory()).isEmpty();
    }

    @Test
    void payerWithOutstandingSplitIsNotSettled() {
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        split(dinner, bob, "50.00", false);
        when(expenseRepo.findAllActivityForUser("alice@test.com")).thenReturn(List.of(dinner));

        ActivityDashboardResponse res = activityService.getUserActivity("alice@test.com");

        assertThat(res.totalExpensesCount()).isEqualTo(1);
        assertThat(res.expensesPaidByUser()).isEqualTo(1);
        assertThat(res.settledExpensesCount()).isZero();
        assertThat(res.totalSpentCombined()).isEqualByComparingTo("50.00");

        ExpenseActivityDTO dto = res.expenseHistory().get(0);
        assertThat(dto.expenseId()).isEqualTo(dinner.getId());
        assertThat(dto.description()).isEqualTo("Dinner");
        assertThat(dto.groupName()).isEqualTo("Trip");
        assertThat(dto.userWasPayer()).isTrue();
        assertThat(dto.amount()).isEqualByComparingTo("100.00");
        assertThat(dto.userShare()).isEqualByComparingTo("50.00");
        assertThat(dto.isSettled()).isFalse();
        assertThat(dto.owesToName()).isNull();
        assertThat(dto.date()).isEqualTo(dinner.getCreatedAt().toString());
    }

    @Test
    void payerIsSettledOnceEveryoneHasPaid() {
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        split(dinner, bob, "50.00", true);
        when(expenseRepo.findAllActivityForUser("alice@test.com")).thenReturn(List.of(dinner));

        ActivityDashboardResponse res = activityService.getUserActivity("alice@test.com");

        assertThat(res.settledExpensesCount()).isEqualTo(1);
        assertThat(res.expenseHistory().get(0).isSettled()).isTrue();
    }

    @Test
    void participantWhoOwesSeesPayerName() {
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        split(dinner, bob, "50.00", false);
        when(expenseRepo.findAllActivityForUser("bob@test.com")).thenReturn(List.of(dinner));

        ActivityDashboardResponse res = activityService.getUserActivity("bob@test.com");

        ExpenseActivityDTO dto = res.expenseHistory().get(0);
        assertThat(dto.userWasPayer()).isFalse();
        assertThat(dto.isSettled()).isFalse();
        assertThat(dto.owesToName()).isEqualTo("Alice");
        assertThat(res.expensesPaidByUser()).isZero();
        assertThat(res.totalSpentCombined()).isEqualByComparingTo("50.00");
    }

    @Test
    void participantWhoPaidIsSettledAndOwesNobody() {
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        split(dinner, bob, "50.00", true);
        when(expenseRepo.findAllActivityForUser("bob@test.com")).thenReturn(List.of(dinner));

        ExpenseActivityDTO dto = activityService.getUserActivity("bob@test.com").expenseHistory().get(0);

        assertThat(dto.isSettled()).isTrue();
        assertThat(dto.owesToName()).isNull();
    }

    @Test
    void payerWithoutOwnSplitHasZeroShare() {
        Expenses gift = expense(trip, alice, "40.00", "Gift");
        split(gift, bob, "40.00", false);
        when(expenseRepo.findAllActivityForUser("alice@test.com")).thenReturn(List.of(gift));

        ActivityDashboardResponse res = activityService.getUserActivity("alice@test.com");

        assertThat(res.expenseHistory().get(0).userShare()).isEqualByComparingTo("0");
        assertThat(res.totalSpentCombined()).isEqualByComparingTo("0");
    }

    @Test
    void nullSettledFlagIsTreatedAsUnsettled() {
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        split(dinner, bob, "50.00", null);
        when(expenseRepo.findAllActivityForUser("bob@test.com")).thenReturn(List.of(dinner));

        ExpenseActivityDTO dto = activityService.getUserActivity("bob@test.com").expenseHistory().get(0);

        assertThat(dto.isSettled()).isFalse();
        assertThat(dto.owesToName()).isEqualTo("Alice");
    }

    @Test
    void aggregatesAcrossMultipleExpenses() {
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        split(dinner, bob, "50.00", true);
        Expenses taxi = expense(trip, bob, "30.00", "Taxi");
        split(taxi, alice, "15.00", false);
        split(taxi, bob, "15.00", true);
        when(expenseRepo.findAllActivityForUser("alice@test.com")).thenReturn(List.of(dinner, taxi));

        ActivityDashboardResponse res = activityService.getUserActivity("alice@test.com");

        assertThat(res.totalExpensesCount()).isEqualTo(2);
        assertThat(res.expensesPaidByUser()).isEqualTo(1);
        assertThat(res.settledExpensesCount()).isEqualTo(1);
        assertThat(res.totalSpentCombined()).isEqualByComparingTo("65.00");
        assertThat(res.expenseHistory()).extracting(ExpenseActivityDTO::description)
                .containsExactly("Dinner", "Taxi");
        assertThat(res.expenseHistory().get(1).owesToName()).isEqualTo("Bob");
    }
}
