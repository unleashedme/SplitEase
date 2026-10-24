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
    @Mock private ExpenseRepo expenseRepo;

    @InjectMocks
    private DashboardService dashboardService;

    private Users alice;
    private Users bob;
    private Users carol;
    private Groups trip;

    @BeforeEach
    void setUp() {
        alice = user("Alice", "alice@test.com");
        bob = user("Bob", "bob@test.com");
        carol = user("Carol", "carol@test.com");
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
    void computesOwedAndOwingFromUnsettledSplits() {
        when(groupMemberRepo.findByUser(alice)).thenReturn(List.of(member(trip, alice, "admin")));

        // Alice paid 90 split 3 ways: Bob owes 30, Carol already paid
        Expenses hotel = expense(trip, alice, "90.00", "Hotel");
        ExpenseSplits aliceHotel = split(hotel, alice, "30.00", true);
        split(hotel, bob, "30.00", false);
        split(hotel, carol, "30.00", true);

        // Bob paid 60 split 3 ways: Alice owes 20; Carol's debt to Bob is not Alice's concern
        Expenses food = expense(trip, bob, "60.00", "Food");
        ExpenseSplits aliceFood = split(food, alice, "20.00", false);
        split(food, bob, "20.00", true);
        split(food, carol, "20.00", false);

        when(expenseRepo.findByGroupIn(List.of(trip))).thenReturn(List.of(hotel, food));
        when(expenseSplitRepo.findAllByUserEmail("alice@test.com")).thenReturn(List.of(aliceHotel, aliceFood));

        DashboardSummaryDTO dto = dashboardService.getDashboardSummary("alice@test.com");

        assertThat(dto.amountOwedToUser()).isEqualByComparingTo("30.00");
        assertThat(dto.amountUserOwes()).isEqualByComparingTo("20.00");
        assertThat(dto.totalUserExpense()).isEqualByComparingTo("50.00");
    }

    @Test
    void ignoresNullShareAmountsInTotal() {
        when(groupMemberRepo.findByUser(alice)).thenReturn(List.of(member(trip, alice, "admin")));
        when(expenseRepo.findByGroupIn(List.of(trip))).thenReturn(List.of());
        ExpenseSplits broken = new ExpenseSplits();
        ExpenseSplits ok = new ExpenseSplits();
        ok.setShareAmount(new BigDecimal("12.50"));
        when(expenseSplitRepo.findAllByUserEmail("alice@test.com")).thenReturn(List.of(broken, ok));

        DashboardSummaryDTO dto = dashboardService.getDashboardSummary("alice@test.com");

        assertThat(dto.totalUserExpense()).isEqualByComparingTo("12.50");
        assertThat(dto.amountOwedToUser()).isEqualByComparingTo("0");
        assertThat(dto.amountUserOwes()).isEqualByComparingTo("0");
    }

    @Test
    void fullySettledExpensesContributeNothingToBalances() {
        when(groupMemberRepo.findByUser(alice)).thenReturn(List.of(member(trip, alice, "admin")));
        Expenses hotel = expense(trip, alice, "90.00", "Hotel");
        ExpenseSplits aliceHotel = split(hotel, alice, "45.00", true);
        split(hotel, bob, "45.00", true);
        when(expenseRepo.findByGroupIn(List.of(trip))).thenReturn(List.of(hotel));
        when(expenseSplitRepo.findAllByUserEmail("alice@test.com")).thenReturn(List.of(aliceHotel));

        DashboardSummaryDTO dto = dashboardService.getDashboardSummary("alice@test.com");

        assertThat(dto.amountOwedToUser()).isEqualByComparingTo("0");
        assertThat(dto.amountUserOwes()).isEqualByComparingTo("0");
        assertThat(dto.totalUserExpense()).isEqualByComparingTo("45.00");
    }
}
