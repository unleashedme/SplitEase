package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.RecordSettlementRequest;
import com.example.splitease.requestAndResponse.SettlementSummaryResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Runs against an in-memory database, since balances come straight from the stored splits and payments. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SettlementService.class)
class SettlementServiceTest {

    @Autowired private SettlementService settlementService;
    @Autowired private EntityManager em;
    @Autowired private UserRepo userRepo;
    @Autowired private GroupRepo groupRepo;
    @Autowired private GroupMemberRepo groupMemberRepo;
    @Autowired private ExpenseRepo expenseRepo;
    @Autowired private ExpenseSplitRepo expenseSplitRepo;
    @Autowired private SettlementRepo settlementRepo;

    @MockitoBean private NotificationService notificationService;

    private Users alice;
    private Users bob;
    private Users carol;
    private Users dave;
    private Groups trip;

    private Users newUser(String name, String phone) {
        Users u = new Users();
        u.setName(name);
        u.setEmail(name.toLowerCase() + "@test.com");
        u.setPhone(phone);
        u.setPassword("pw");
        return userRepo.save(u);
    }

    private Groups newGroup(String name, Users... members) {
        Groups g = new Groups();
        g.setName(name);
        g.setCreatedBy(members[0]);
        groupRepo.save(g);
        for (Users u : members) {
            GroupMembers gm = new GroupMembers();
            gm.setGroup(g);
            gm.setUser(u);
            gm.setRole("member");
            groupMemberRepo.save(gm);
        }
        return g;
    }

    /** payer paid for the listed people, each owing [share]; the payer's own share counts as settled. */
    private void expense(Groups group, Users payer, String share, Users... participants) {
        Expenses e = new Expenses();
        e.setGroup(group);
        e.setPayer(payer);
        e.setAmount(new BigDecimal(share).multiply(BigDecimal.valueOf(participants.length)));
        e.setDescription("exp");
        expenseRepo.save(e);
        for (Users p : participants) {
            ExpenseSplits s = new ExpenseSplits();
            s.setExpense(e);
            s.setUser(p);
            s.setShareAmount(new BigDecimal(share));
            s.setSettled(p.getId().equals(payer.getId()));
            expenseSplitRepo.save(s);
        }
    }

    private void pay(Users from, Users to, String amount) {
        RecordSettlementRequest request = new RecordSettlementRequest();
        request.setToUserId(to.getId());
        request.setAmount(new BigDecimal(amount));
        request.setNote("upi");
        settlementService.recordSettlement(request, from.getEmail());
        em.flush();
        em.clear();
    }

    private List<SettlementSummaryResponse> payables(Users user) {
        return settlementService.getMyPayables(user.getEmail());
    }

    private static SettlementSummaryResponse owes(Users to, String amount) {
        return new SettlementSummaryResponse(to.getId().toString(), to.getName(), new BigDecimal(amount));
    }

    @BeforeEach
    void setUp() {
        alice = newUser("Alice", "1");
        bob = newUser("Bob", "2");
        carol = newUser("Carol", "3");
        dave = newUser("Dave", "4");
        trip = newGroup("Trip", alice, bob, carol, dave);
    }

    @Test
    void middlemanIsCutOut() {
        expense(trip, alice, "30.00", bob);   // Bob owes Alice 30
        expense(trip, bob, "30.00", carol);   // Carol owes Bob 30

        assertThat(payables(carol)).containsExactly(owes(alice, "30.00"));
        assertThat(payables(bob)).isEmpty();
        assertThat(payables(alice)).isEmpty();
    }

    @Test
    void groupWithManyDebtsNeedsFewerPayments() {
        // Each of the four paid for one dinner split four ways, and Dave also bought Alice a drink
        expense(trip, alice, "10.00", alice, bob, carol, dave);
        expense(trip, bob, "10.00", alice, bob, carol, dave);
        expense(trip, carol, "10.00", alice, bob, carol, dave);
        expense(trip, dave, "10.00", alice, bob, carol, dave);
        expense(trip, dave, "25.00", alice);

        // 13 debts between pairs, but only one payment is actually needed
        assertThat(payables(alice)).containsExactly(owes(dave, "25.00"));
        assertThat(payables(bob)).isEmpty();
        assertThat(payables(carol)).isEmpty();
        assertThat(payables(dave)).isEmpty();
    }

    @Test
    void payingTheLastDebtSettlesEveryExpenseInTheGroup() {
        expense(trip, alice, "30.00", bob);
        expense(trip, bob, "30.00", carol);

        pay(carol, alice, "30.00");

        assertThat(payables(carol)).isEmpty();
        assertThat(expenseSplitRepo.findByExpenseGroupAndSettledFalse(trip)).isEmpty();
        assertThat(settlementRepo.findOpenByGroup(trip)).isEmpty();
        assertThat(settlementRepo.findAll()).singleElement().satisfies(s -> {
            assertThat(s.getFromUser().getId()).isEqualTo(carol.getId());
            assertThat(s.getToUser().getId()).isEqualTo(alice.getId());
            assertThat(s.getGroup().getId()).isEqualTo(trip.getId());
            assertThat(s.getNote()).isEqualTo("upi");
            assertThat(s.getCleared()).isTrue();
        });
        verify(notificationService, never()).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    void partPaidGroupKeepsOtherDebtsOpen() {
        expense(trip, alice, "40.00", bob, carol); // Bob and Carol each owe Alice 40

        pay(bob, alice, "40.00");

        assertThat(payables(bob)).isEmpty();
        assertThat(payables(carol)).containsExactly(owes(alice, "40.00"));
        assertThat(settlementService.groupBalances(trip))
                .containsEntry(bob.getId(), new BigDecimal("0.00"))
                .containsEntry(alice.getId(), new BigDecimal("40.00"));
        // Splits stay open until the whole group is square
        assertThat(expenseSplitRepo.findByExpenseGroupAndSettledFalse(trip)).hasSize(2);

        pay(carol, alice, "40.00");

        assertThat(expenseSplitRepo.findByExpenseGroupAndSettledFalse(trip)).isEmpty();
    }

    @Test
    void debtsInDifferentGroupsAreNettedIntoOnePayment() {
        Groups flat = newGroup("Flat", alice, bob);
        expense(trip, bob, "50.00", alice);  // Alice owes Bob 50 in Trip
        expense(flat, alice, "20.00", bob);  // Bob owes Alice 20 in Flat

        assertThat(payables(alice)).containsExactly(owes(bob, "30.00"));
        assertThat(payables(bob)).isEmpty();

        pay(alice, bob, "30.00");

        // Stored per group so each group's balance is right on its own
        List<Settlements> payments = settlementRepo.findAll();
        assertThat(payments).hasSize(2);
        assertThat(payments).anySatisfy(s -> {
            assertThat(s.getGroup().getId()).isEqualTo(trip.getId());
            assertThat(s.getFromUser().getId()).isEqualTo(alice.getId());
            assertThat(s.getAmount()).isEqualByComparingTo("50.00");
        });
        assertThat(payments).anySatisfy(s -> {
            assertThat(s.getGroup().getId()).isEqualTo(flat.getId());
            assertThat(s.getFromUser().getId()).isEqualTo(bob.getId());
            assertThat(s.getAmount()).isEqualByComparingTo("20.00");
            assertThat(s.getNote()).isEqualTo("Netted against Alice's payment of ₹30.00");
        });
        assertThat(expenseSplitRepo.findByExpenseGroupAndSettledFalse(trip)).isEmpty();
        assertThat(expenseSplitRepo.findByExpenseGroupAndSettledFalse(flat)).isEmpty();
    }

    @Test
    void nobodyIsAskedToPaySomeoneOutsideTheirGroups() {
        Users erin = newUser("Erin", "5");
        Groups work = newGroup("Work", bob, erin);
        expense(trip, alice, "30.00", bob);  // Bob owes Alice in Trip
        expense(work, bob, "30.00", erin);   // Erin owes Bob in Work

        // Simplifying across groups would have Erin pay Alice, who she doesn't share a group with
        assertThat(payables(erin)).containsExactly(owes(bob, "30.00"));
        assertThat(payables(bob)).containsExactly(owes(alice, "30.00"));
    }

    @Test
    void debtTotalsMatchThePayables() {
        expense(trip, alice, "30.00", bob, carol);
        expense(trip, bob, "10.00", alice);

        // Bob: owes Alice 30, is owed 10 by Alice -> owes 20. Carol owes Alice 30.
        assertThat(settlementService.debtTotals(alice))
                .isEqualTo(new SettlementService.DebtTotals(BigDecimal.ZERO, new BigDecimal("50.00")));
        SettlementService.DebtTotals forBob = settlementService.debtTotals(bob);
        assertThat(forBob.owes()).isEqualByComparingTo("20.00");
        assertThat(forBob.owed()).isEqualByComparingTo("0");
    }

    @Test
    void paymentsFromBeforeSimplificationAreIgnored() {
        expense(trip, alice, "30.00", bob);
        Settlements legacy = new Settlements();
        legacy.setFromUser(bob);
        legacy.setToUser(alice);
        legacy.setAmount(new BigDecimal("999.00")); // already applied by marking splits settled back then
        settlementRepo.save(legacy);

        assertThat(payables(bob)).containsExactly(owes(alice, "30.00"));
    }

    @Test
    void groupThatEvensOutWithoutPaymentsIsCleared() {
        expense(trip, alice, "25.00", bob);
        expense(trip, bob, "25.00", alice);

        settlementService.clearIfSettled(trip);

        assertThat(expenseSplitRepo.findByExpenseGroupAndSettledFalse(trip)).isEmpty();
    }

    @Test
    void staleAmountIsRejected() {
        expense(trip, alice, "30.00", bob);

        assertThatThrownBy(() -> pay(bob, alice, "20.00"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getReason()).contains("you now owe Alice ₹30.00");
                });
        assertThat(settlementRepo.findAll()).isEmpty();
    }

    @Test
    void payingSomeoneYouDoNotOweIsRejected() {
        expense(trip, alice, "30.00", bob);

        assertThatThrownBy(() -> pay(alice, bob, "30.00"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void invalidRequestsAreRejected() {
        expense(trip, alice, "30.00", bob);

        assertThatThrownBy(() -> pay(bob, alice, "0"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        RecordSettlementRequest unknown = new RecordSettlementRequest();
        unknown.setToUserId(UUID.randomUUID());
        unknown.setAmount(new BigDecimal("30.00"));
        assertThatThrownBy(() -> settlementService.recordSettlement(unknown, bob.getEmail()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void receiverIsNotifiedAndAFailedNotificationDoesNotUndoThePayment() {
        alice.setFcmToken("alice-token");
        userRepo.save(alice);
        expense(trip, alice, "30.00", bob);
        doThrow(new RuntimeException("FCM down")).when(notificationService)
                .sendPushNotification(anyString(), anyString(), anyString());

        pay(bob, alice, "30.00");

        verify(notificationService).sendPushNotification(eq("alice-token"), eq("Payment Received"), eq("Bob settled up ₹30.00"));
        assertThat(payables(bob)).isEmpty();
    }
}
