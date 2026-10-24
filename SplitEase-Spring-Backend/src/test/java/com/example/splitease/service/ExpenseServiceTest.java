package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.AddExpenseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.splitease.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private GroupRepo groupRepo;
    @Mock private GroupMemberRepo groupMemberRepo;
    @Mock private ExpenseRepo expenseRepo;
    @Mock private ExpenseSplitRepo expenseSplitRepo;
    @Mock private UserRepo userRepo;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ExpenseService expenseService;

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
    }

    private AddExpenseRequest request(UUID groupId, String amount, String description) {
        AddExpenseRequest req = new AddExpenseRequest();
        req.setGroupId(groupId);
        req.setAmount(amount == null ? null : new BigDecimal(amount));
        req.setDescription(description);
        return req;
    }

    private void groupWithMembers(Users... users) {
        when(groupRepo.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(groupMemberRepo.findByGroupId(trip.getId()))
                .thenReturn(java.util.Arrays.stream(users).map(u -> member(trip, u, "member")).toList());
    }

    @Test
    void addExpenseSplitsEquallyAndMarksPayerSettled() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(request(trip.getId(), "90.00", "Hotel"), "alice@test.com");

        ArgumentCaptor<Expenses> expenseCaptor = ArgumentCaptor.forClass(Expenses.class);
        verify(expenseRepo).save(expenseCaptor.capture());
        Expenses saved = expenseCaptor.getValue();
        assertThat(saved.getGroup()).isSameAs(trip);
        assertThat(saved.getPayer()).isSameAs(alice);
        assertThat(saved.getAmount()).isEqualByComparingTo("90.00");
        assertThat(saved.getDescription()).isEqualTo("Hotel");

        ArgumentCaptor<ExpenseSplits> splitCaptor = ArgumentCaptor.forClass(ExpenseSplits.class);
        verify(expenseSplitRepo, times(3)).save(splitCaptor.capture());
        List<ExpenseSplits> splits = splitCaptor.getAllValues();
        assertThat(splits).allSatisfy(s -> {
            assertThat(s.getExpense()).isSameAs(saved);
            assertThat(s.getShareAmount()).isEqualByComparingTo("30.00");
        });
        assertThat(splits).filteredOn(s -> s.getUser() == alice).singleElement()
                .satisfies(s -> assertThat(s.getSettled()).isTrue());
        assertThat(splits).filteredOn(s -> s.getUser() != alice)
                .hasSize(2)
                .allSatisfy(s -> assertThat(s.getSettled()).isFalse());
    }

    @Test
    void addExpenseRoundsSharesToTwoDecimals() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(request(trip.getId(), "100", "Snacks"), "alice@test.com");

        ArgumentCaptor<ExpenseSplits> splitCaptor = ArgumentCaptor.forClass(ExpenseSplits.class);
        verify(expenseSplitRepo, times(3)).save(splitCaptor.capture());
        assertThat(splitCaptor.getAllValues())
                .allSatisfy(s -> assertThat(s.getShareAmount()).isEqualTo(new BigDecimal("33.33")));
    }

    @Test
    void addExpenseNotifiesOtherMembersWithTokensOnly() {
        alice.setFcmToken("alice-token");
        bob.setFcmToken("bob-token");
        carol.setFcmToken("");
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(request(trip.getId(), "50", "Taxi"), "alice@test.com");

        verify(notificationService).sendPushNotification(
                eq("bob-token"), eq("New Expense in Trip"), contains("Alice added ₹50 for Taxi"));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void addExpenseFailsWhenGroupMissing() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        UUID missing = UUID.randomUUID();
        when(groupRepo.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.addExpense(request(missing, "10", "x"), "alice@test.com"))
                .hasMessageContaining("Group not found");
        verifyNoInteractions(expenseRepo, expenseSplitRepo, notificationService);
    }

    @Test
    void addExpenseFailsWhenGroupHasNoMembers() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers();

        assertThatThrownBy(() -> expenseService.addExpense(request(trip.getId(), "10", "x"), "alice@test.com"))
                .hasMessageContaining("Group has no members");
        verifyNoInteractions(expenseRepo, expenseSplitRepo);
    }

    @Test
    void addExpenseRejectsPayerWhoIsNotAGroupMember() {
        Users mallory = user("Mallory", "mallory@test.com");
        when(userRepo.findByEmail("mallory@test.com")).thenReturn(mallory);
        groupWithMembers(alice, bob);

        assertThatThrownBy(() -> expenseService.addExpense(request(trip.getId(), "10", "x"), "mallory@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(expenseRepo, expenseSplitRepo, notificationService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-5.00"})
    void addExpenseRejectsNonPositiveAmount(String amount) {
        assertThatThrownBy(() -> expenseService.addExpense(request(trip.getId(), amount, "x"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(expenseRepo, expenseSplitRepo, notificationService);
    }

    @Test
    void deleteExpenseByPayerDeletesIt() throws Exception {
        Expenses dinner = expense(trip, alice, "10", "Dinner");
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));

        expenseService.deleteExpense(dinner.getId(), "alice@test.com");

        verify(expenseRepo).delete(dinner);
    }

    @Test
    void deleteExpenseByNonPayerIsDenied() {
        Expenses dinner = expense(trip, alice, "10", "Dinner");
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));

        assertThatThrownBy(() -> expenseService.deleteExpense(dinner.getId(), "bob@test.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseRepo, never()).delete(any());
    }

    @Test
    void deleteMissingExpenseIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(expenseRepo.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.deleteExpense(missing, "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
