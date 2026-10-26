package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.ExpenseReceiptRepo;
import com.example.splitease.repo.ExpenseRepo;
import com.example.splitease.requestAndResponse.ActivityDashboardResponse;
import com.example.splitease.requestAndResponse.ExpenseActivityDTO;
import com.example.splitease.requestAndResponse.SplitShareDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.example.splitease.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ExpenseRepo expenseRepo;

    @Mock
    private ExpenseReceiptRepo expenseReceiptRepo;

    // Returns no balances unless a test sets them, so "settled" comes from the split flags alone
    @Mock
    private SettlementService settlementService;

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
    void participantWhoIsSquareInTheGroupCountsAsSettled() {
        // Bob's split is still open, but he paid someone else in the group what the plan said
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        split(dinner, bob, "50.00", false);
        when(expenseRepo.findAllActivityForUser("bob@test.com")).thenReturn(List.of(dinner));
        when(settlementService.groupBalances(trip)).thenReturn(Map.of(
                alice.getId(), new BigDecimal("30.00"),
                bob.getId(), new BigDecimal("0.00")
        ));

        ExpenseActivityDTO dto = activityService.getUserActivity("bob@test.com").expenseHistory().get(0);

        assertThat(dto.isSettled()).isTrue();
        assertThat(dto.owesToName()).isNull();
        assertThat(dto.shares()).filteredOn(SplitShareDTO::isCurrentUser)
                .singleElement().satisfies(s -> assertThat(s.isSettled()).isTrue());
        // Alice is still owed money in the group, so her side isn't settled
        assertThat(dto.shares()).filteredOn(SplitShareDTO::isPayer).singleElement()
                .satisfies(s -> assertThat(s.isSettled()).isTrue()); // her own share is marked settled
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
    void listsEveryonesShareWithPayerFirst() {
        Users carol = user("Carol", "carol@test.com");
        Expenses dinner = expense(trip, bob, "100.00", "Dinner");
        dinner.setSplitType(SplitType.EXACT);
        split(dinner, carol, "30.00", true);
        split(dinner, alice, "50.00", false);
        split(dinner, bob, "20.00", true);
        when(expenseRepo.findAllActivityForUser("alice@test.com")).thenReturn(List.of(dinner));

        ExpenseActivityDTO dto = activityService.getUserActivity("alice@test.com").expenseHistory().get(0);

        assertThat(dto.payerName()).isEqualTo("Bob");
        assertThat(dto.splitType()).isEqualTo("EXACT");
        assertThat(dto.shares()).containsExactly(
                new SplitShareDTO("Bob", new BigDecimal("20.00"), true, false, true),
                new SplitShareDTO("Alice", new BigDecimal("50.00"), false, true, false),
                new SplitShareDTO("Carol", new BigDecimal("30.00"), false, false, true)
        );
    }

    @Test
    void expensesFromBeforeCustomSplitsCountAsEqual() {
        Expenses dinner = expense(trip, alice, "100.00", "Dinner");
        split(dinner, alice, "50.00", true);
        when(expenseRepo.findAllActivityForUser("alice@test.com")).thenReturn(List.of(dinner));

        assertThat(activityService.getUserActivity("alice@test.com").expenseHistory().get(0).splitType()).isEqualTo("EQUAL");
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
