package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.AddExpenseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @Mock private SettlementService settlementService;

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
    void addExpensePayerAbsorbsRoundingRemainder() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(request(trip.getId(), "100", "Snacks"), "alice@test.com");

        ArgumentCaptor<ExpenseSplits> splitCaptor = ArgumentCaptor.forClass(ExpenseSplits.class);
        verify(expenseSplitRepo, times(3)).save(splitCaptor.capture());
        List<ExpenseSplits> splits = splitCaptor.getAllValues();
        assertThat(splits).filteredOn(s -> s.getUser() != alice)
                .allSatisfy(s -> assertThat(s.getShareAmount()).isEqualByComparingTo("33.33"));
        assertThat(splits).filteredOn(s -> s.getUser() == alice).singleElement()
                .satisfies(s -> assertThat(s.getShareAmount()).isEqualByComparingTo("33.34"));
    }

    @ParameterizedTest
    @CsvSource({"100, 3", "100.01, 3", "0.01, 3", "0.05, 4", "999.99, 7", "10, 2", "1, 1"})
    void addExpenseSharesAlwaysSumToAmount(String amount, int memberCount) {
        Users[] users = new Users[memberCount];
        users[0] = alice;
        for (int i = 1; i < memberCount; i++) users[i] = user("U" + i, "u" + i + "@test.com");
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(users);

        expenseService.addExpense(request(trip.getId(), amount, "x"), "alice@test.com");

        ArgumentCaptor<ExpenseSplits> splitCaptor = ArgumentCaptor.forClass(ExpenseSplits.class);
        verify(expenseSplitRepo, times(memberCount)).save(splitCaptor.capture());
        List<ExpenseSplits> splits = splitCaptor.getAllValues();
        BigDecimal total = splits.stream().map(ExpenseSplits::getShareAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(amount);

        // Non-payers never pay more than the payer, and differ from the payer by under 1 paisa per member
        BigDecimal payerShare = splits.stream().filter(s -> s.getUser() == alice).findFirst().orElseThrow().getShareAmount();
        assertThat(splits).filteredOn(s -> s.getUser() != alice).allSatisfy(s -> {
            assertThat(s.getShareAmount()).isLessThanOrEqualTo(payerShare);
            assertThat(s.getShareAmount().scale()).isLessThanOrEqualTo(2);
        });
        assertThat(payerShare.subtract(splits.get(splits.size() - 1).getShareAmount()))
                .isLessThan(new BigDecimal("0.01").multiply(BigDecimal.valueOf(memberCount)));
    }

    private AddExpenseRequest customRequest(String amount, SplitType type, Object... userValuePairs) {
        AddExpenseRequest req = request(trip.getId(), amount, "Custom");
        req.setSplitType(type);
        List<AddExpenseRequest.SplitEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < userValuePairs.length; i += 2) {
            Object value = userValuePairs[i + 1];
            entries.add(new AddExpenseRequest.SplitEntry(((Users) userValuePairs[i]).getId(),
                    value == null ? null : new BigDecimal(value.toString())));
        }
        req.setSplits(entries);
        return req;
    }

    private List<ExpenseSplits> savedSplits(int expectedCount) {
        ArgumentCaptor<ExpenseSplits> splitCaptor = ArgumentCaptor.forClass(ExpenseSplits.class);
        verify(expenseSplitRepo, times(expectedCount)).save(splitCaptor.capture());
        return splitCaptor.getAllValues();
    }

    private BigDecimal shareOf(List<ExpenseSplits> splits, Users u) {
        return splits.stream().filter(s -> s.getUser() == u).findFirst().orElseThrow().getShareAmount();
    }

    @Test
    void addExpenseEqualSplitBetweenChosenMembersOnly() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(customRequest("100", SplitType.EQUAL, alice, null, bob, null), "alice@test.com");

        List<ExpenseSplits> splits = savedSplits(2);
        assertThat(splits).extracting(ExpenseSplits::getUser).containsExactly(alice, bob);
        assertThat(shareOf(splits, alice)).isEqualByComparingTo("50.00");
        assertThat(shareOf(splits, bob)).isEqualByComparingTo("50.00");
    }

    @Test
    void addExpenseExactSplitUsesGivenAmountsAndSkipsZeroes() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(customRequest("100", SplitType.EXACT, alice, "70", bob, "30.00", carol, "0"), "alice@test.com");

        ArgumentCaptor<Expenses> expenseCaptor = ArgumentCaptor.forClass(Expenses.class);
        verify(expenseRepo).save(expenseCaptor.capture());
        assertThat(expenseCaptor.getValue().getSplitType()).isEqualTo(SplitType.EXACT);

        List<ExpenseSplits> splits = savedSplits(2);
        assertThat(shareOf(splits, alice)).isEqualByComparingTo("70");
        assertThat(shareOf(splits, bob)).isEqualByComparingTo("30");
        assertThat(splits).filteredOn(s -> s.getUser() == bob).singleElement()
                .satisfies(s -> assertThat(s.getSettled()).isFalse());
    }

    @Test
    void addExpenseWherePayerIsNotSharingLeavesOthersOwingEverything() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(customRequest("100", SplitType.EXACT, bob, "60", carol, "40"), "alice@test.com");

        List<ExpenseSplits> splits = savedSplits(2);
        assertThat(splits).extracting(ExpenseSplits::getUser).containsExactly(bob, carol);
        assertThat(splits).allSatisfy(s -> assertThat(s.getSettled()).isFalse());
    }

    @Test
    void addExpensePercentageSplit() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(customRequest("250", SplitType.PERCENTAGE, alice, "50", bob, "30", carol, "20"), "alice@test.com");

        List<ExpenseSplits> splits = savedSplits(3);
        assertThat(shareOf(splits, alice)).isEqualByComparingTo("125.00");
        assertThat(shareOf(splits, bob)).isEqualByComparingTo("75.00");
        assertThat(shareOf(splits, carol)).isEqualByComparingTo("50.00");
    }

    @Test
    void addExpenseSharesSplitGivesRoundingRemainderToPayer() {
        when(userRepo.findByEmail("bob@test.com")).thenReturn(bob);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(customRequest("100", SplitType.SHARES, alice, "1", bob, "1", carol, "1"), "bob@test.com");

        List<ExpenseSplits> splits = savedSplits(3);
        assertThat(shareOf(splits, alice)).isEqualByComparingTo("33.33");
        assertThat(shareOf(splits, bob)).isEqualByComparingTo("33.34");
        assertThat(shareOf(splits, carol)).isEqualByComparingTo("33.33");
    }

    @Test
    void addExpenseSharesSplitWeightsShares() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(customRequest("100", SplitType.SHARES, alice, "2", bob, "1", carol, "1"), "alice@test.com");

        List<ExpenseSplits> splits = savedSplits(3);
        assertThat(shareOf(splits, alice)).isEqualByComparingTo("50.00");
        assertThat(shareOf(splits, bob)).isEqualByComparingTo("25.00");
        assertThat(shareOf(splits, carol)).isEqualByComparingTo("25.00");
    }

    @Test
    void addExpenseRemainderGoesToFirstParticipantWhenPayerNotSharing() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob, carol);

        expenseService.addExpense(customRequest("0.05", SplitType.EQUAL, bob, null, carol, null), "alice@test.com");

        List<ExpenseSplits> splits = savedSplits(2);
        assertThat(shareOf(splits, bob)).isEqualByComparingTo("0.03");
        assertThat(shareOf(splits, carol)).isEqualByComparingTo("0.02");
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidSplits() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("exact total too low", SplitType.EXACT, new String[]{"50", "40"}),
                org.junit.jupiter.params.provider.Arguments.of("exact with fractions of paise", SplitType.EXACT, new String[]{"50.005", "49.995"}),
                org.junit.jupiter.params.provider.Arguments.of("percentages not 100", SplitType.PERCENTAGE, new String[]{"50", "40"}),
                org.junit.jupiter.params.provider.Arguments.of("negative value", SplitType.SHARES, new String[]{"2", "-1"}),
                org.junit.jupiter.params.provider.Arguments.of("missing value", SplitType.EXACT, new String[]{"100", null}),
                org.junit.jupiter.params.provider.Arguments.of("nobody sharing", SplitType.SHARES, new String[]{"0", "0"})
        );
    }

    @ParameterizedTest(name = "{0}")
    @org.junit.jupiter.params.provider.MethodSource("invalidSplits")
    void addExpenseRejectsInvalidCustomSplit(String name, SplitType type, String[] values) {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob);

        assertThatThrownBy(() -> expenseService.addExpense(
                customRequest("100", type, alice, values[0], bob, values[1]), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(expenseRepo, expenseSplitRepo, notificationService);
    }

    @Test
    void addExpenseRejectsSplitWithNonMember() {
        Users mallory = user("Mallory", "mallory@test.com");
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob);

        assertThatThrownBy(() -> expenseService.addExpense(
                customRequest("100", SplitType.EXACT, alice, "50", mallory, "50"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(expenseRepo, expenseSplitRepo);
    }

    @Test
    void addExpenseRejectsDuplicateMemberInSplit() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob);

        assertThatThrownBy(() -> expenseService.addExpense(
                customRequest("100", SplitType.EXACT, bob, "50", bob, "50"), "alice@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("more than once");
        verifyNoInteractions(expenseRepo, expenseSplitRepo);
    }

    @Test
    void addExpenseRejectsCustomTypeWithoutSplits() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        groupWithMembers(alice, bob);
        AddExpenseRequest req = request(trip.getId(), "100", "x");
        req.setSplitType(SplitType.PERCENTAGE);

        assertThatThrownBy(() -> expenseService.addExpense(req, "alice@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Split details are required");
        verifyNoInteractions(expenseRepo, expenseSplitRepo);
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
