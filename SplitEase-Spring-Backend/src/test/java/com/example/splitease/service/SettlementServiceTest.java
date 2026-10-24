package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.RecordSettlementRequest;
import com.example.splitease.requestAndResponse.SettlementSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.splitease.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock private GroupRepo groupRepo;
    @Mock private GroupMemberRepo groupMemberRepo;
    @Mock private SettlementRepo settlementRepo;
    @Mock private ExpenseRepo expenseRepo;
    @Mock private ExpenseSplitRepo expenseSplitRepo;
    @Mock private UserRepo userRepo;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private SettlementService settlementService;

    private Users alice;
    private Users bob;

    @BeforeEach
    void setUp() {
        alice = user("Alice", "alice@test.com");
        bob = user("Bob", "bob@test.com");
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
    }

    @Test
    void getMyPayablesReturnsOnlyPositiveBalances() {
        UUID bobId = bob.getId();
        UUID carolId = UUID.randomUUID();
        UUID daveId = UUID.randomUUID();
        when(expenseSplitRepo.calculateNetBalances(alice.getId())).thenReturn(List.of(
                new Object[]{bobId, "Bob", new BigDecimal("25.50")},
                new Object[]{carolId, "Carol", new BigDecimal("-10.00")},
                new Object[]{daveId, "Dave", BigDecimal.ZERO}
        ));

        List<SettlementSummaryResponse> payables = settlementService.getMyPayables("alice@test.com");

        assertThat(payables).singleElement().satisfies(p -> {
            assertThat(p.toUserId()).isEqualTo(bobId.toString());
            assertThat(p.toUserName()).isEqualTo("Bob");
            assertThat(p.amount()).isEqualByComparingTo("25.50");
        });
    }

    @Test
    void getMyPayablesAcceptsAnyNumericBalanceType() {
        when(expenseSplitRepo.calculateNetBalances(alice.getId())).thenReturn(List.<Object[]>of(
                new Object[]{bob.getId(), "Bob", 15.0d}
        ));

        List<SettlementSummaryResponse> payables = settlementService.getMyPayables("alice@test.com");

        assertThat(payables).singleElement()
                .satisfies(p -> assertThat(p.amount()).isEqualByComparingTo("15"));
    }

    @Test
    void getMyPayablesEmptyWhenNoBalances() {
        when(expenseSplitRepo.calculateNetBalances(alice.getId())).thenReturn(List.of());

        assertThat(settlementService.getMyPayables("alice@test.com")).isEmpty();
    }

    private RecordSettlementRequest settle(UUID to, String amount, String note) {
        RecordSettlementRequest req = new RecordSettlementRequest();
        req.setToUserId(to);
        req.setAmount(new BigDecimal(amount));
        req.setNote(note);
        return req;
    }

    @Test
    void recordSettlementSavesAuditRecordAndSettlesSplits() {
        when(userRepo.findById(bob.getId())).thenReturn(Optional.of(bob));
        Groups trip = group("Trip", bob);
        Expenses dinner = expense(trip, bob, "40", "Dinner");
        ExpenseSplits aliceOwesBob = split(dinner, alice, "20", false);
        Expenses taxi = expense(trip, alice, "10", "Taxi");
        ExpenseSplits bobOwesAlice = split(taxi, bob, "5", false);
        when(expenseSplitRepo.findAllUnsettledBetweenUsers(alice.getId(), bob.getId()))
                .thenReturn(List.of(aliceOwesBob, bobOwesAlice));

        settlementService.recordSettlement(settle(bob.getId(), "15", "cash"), "alice@test.com");

        ArgumentCaptor<Settlements> captor = ArgumentCaptor.forClass(Settlements.class);
        verify(settlementRepo).save(captor.capture());
        Settlements s = captor.getValue();
        assertThat(s.getFromUser()).isSameAs(alice);
        assertThat(s.getToUser()).isSameAs(bob);
        assertThat(s.getAmount()).isEqualByComparingTo("15");
        assertThat(s.getNote()).isEqualTo("cash");
        assertThat(s.getCreatedAt()).isNotNull();

        assertThat(aliceOwesBob.getSettled()).isTrue();
        assertThat(bobOwesAlice.getSettled()).isTrue();
        verify(expenseSplitRepo).saveAll(List.of(aliceOwesBob, bobOwesAlice));
    }

    @Test
    void recordSettlementNotifiesReceiverWhenTokenPresent() {
        bob.setFcmToken("bob-token");
        when(userRepo.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(expenseSplitRepo.findAllUnsettledBetweenUsers(any(), any())).thenReturn(List.of());

        settlementService.recordSettlement(settle(bob.getId(), "15", null), "alice@test.com");

        verify(notificationService).sendPushNotification("bob-token", "Payment Received", "Alice settled up ₹15");
    }

    @Test
    void recordSettlementSkipsNotificationWithoutToken() {
        bob.setFcmToken("   ");
        when(userRepo.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(expenseSplitRepo.findAllUnsettledBetweenUsers(any(), any())).thenReturn(List.of());

        settlementService.recordSettlement(settle(bob.getId(), "15", null), "alice@test.com");

        verifyNoInteractions(notificationService);
    }

    @Test
    void recordSettlementSurvivesNotificationFailure() {
        bob.setFcmToken("bob-token");
        when(userRepo.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(expenseSplitRepo.findAllUnsettledBetweenUsers(any(), any())).thenReturn(List.of());
        doThrow(new IllegalStateException("firebase down"))
                .when(notificationService).sendPushNotification(anyString(), anyString(), anyString());

        assertThatCode(() -> settlementService.recordSettlement(settle(bob.getId(), "15", null), "alice@test.com"))
                .doesNotThrowAnyException();
        verify(settlementRepo).save(any());
    }

    @Test
    void recordSettlementFailsForUnknownReceiver() {
        UUID missing = UUID.randomUUID();
        when(userRepo.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.recordSettlement(settle(missing, "15", null), "alice@test.com"))
                .hasMessageContaining("Receiver not found");
        verifyNoInteractions(settlementRepo, expenseSplitRepo, notificationService);
    }
}
