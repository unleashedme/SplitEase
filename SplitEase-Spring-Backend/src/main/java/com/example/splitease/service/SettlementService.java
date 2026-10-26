package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.RecordSettlementRequest;
import com.example.splitease.requestAndResponse.SettlementSummaryResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

/**
 * Works out who should pay whom, and records payments.
 *
 * Debts are simplified within each group (see {@link DebtSimplifier}), so members may be asked to
 * pay someone other than the person who paid for them, but never someone outside their groups.
 * What two people owe each other across all their shared groups is then netted into one payment.
 */
@Service
public class SettlementService {

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private SettlementRepo settlementRepo;

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;

    /** What the user owes and is owed in total, after simplifying. */
    public record DebtTotals(BigDecimal owes, BigDecimal owed) {}

    /** What one other person and the user owe each other, netted across their shared groups. */
    private static final class PairDebt {
        final Users other;
        BigDecimal amount = BigDecimal.ZERO; // positive: the user owes them; negative: they owe the user
        final Map<UUID, BigDecimal> byGroupId = new LinkedHashMap<>(); // same sign convention, per group
        final Map<UUID, Groups> groups = new HashMap<>();

        PairDebt(Users other) {
            this.other = other;
        }

        void add(Groups group, BigDecimal signedAmount) {
            amount = amount.add(signedAmount);
            byGroupId.merge(group.getId(), signedAmount, BigDecimal::add);
            groups.put(group.getId(), group);
        }
    }

    /** A group's balances plus the people they belong to. */
    private record GroupLedger(Map<UUID, BigDecimal> balances, Map<UUID, Users> people) {}

    /**
     * Each member's net balance in the group: positive means they are owed money, negative means
     * they owe. Members with outstanding activity are included even when their balance is zero.
     */
    public Map<UUID, BigDecimal> groupBalances(Groups group) {
        return ledger(group).balances();
    }

    private GroupLedger ledger(Groups group) {
        Map<UUID, BigDecimal> balances = new HashMap<>();
        Map<UUID, Users> people = new HashMap<>();

        for (ExpenseSplits split : expenseSplitRepo.findByExpenseGroupAndSettledFalse(group)) {
            Users payer = split.getExpense().getPayer();
            Users user = split.getUser();
            // A payer's own share is something they owe themselves
            if (payer.getId().equals(user.getId())) continue;
            credit(balances, people, payer, split.getShareAmount());
            credit(balances, people, user, split.getShareAmount().negate());
        }
        // Paying money back moves the payer's balance up and the receiver's down
        for (Settlements payment : settlementRepo.findOpenByGroup(group)) {
            credit(balances, people, payment.getFromUser(), payment.getAmount());
            credit(balances, people, payment.getToUser(), payment.getAmount().negate());
        }
        return new GroupLedger(balances, people);
    }

    private static void credit(Map<UUID, BigDecimal> balances, Map<UUID, Users> people, Users user, BigDecimal amount) {
        balances.merge(user.getId(), amount, BigDecimal::add);
        people.putIfAbsent(user.getId(), user);
    }

    /** Everyone the user owes or is owed by, netted across all of the user's groups. */
    private Map<UUID, PairDebt> debtsOf(Users user) {
        Map<UUID, PairDebt> debts = new HashMap<>();
        for (GroupMembers membership : groupMemberRepo.findByUser(user)) {
            Groups group = membership.getGroup();
            GroupLedger ledger = ledger(group);

            for (DebtSimplifier.Transfer transfer : DebtSimplifier.simplify(ledger.balances())) {
                if (transfer.from().equals(user.getId())) {
                    debts.computeIfAbsent(transfer.to(), id -> new PairDebt(ledger.people().get(id)))
                            .add(group, transfer.amount());
                } else if (transfer.to().equals(user.getId())) {
                    debts.computeIfAbsent(transfer.from(), id -> new PairDebt(ledger.people().get(id)))
                            .add(group, transfer.amount().negate());
                }
            }
        }
        return debts;
    }

    public List<SettlementSummaryResponse> getMyPayables(String userEmail) {
        Users currentUser = userRepo.findByEmail(userEmail);

        return debtsOf(currentUser).values().stream()
                .filter(debt -> debt.amount.signum() > 0)
                .sorted(Comparator.comparing(debt -> debt.other.getName(), String.CASE_INSENSITIVE_ORDER))
                .map(debt -> new SettlementSummaryResponse(debt.other.getId().toString(), debt.other.getName(), debt.amount))
                .toList();
    }

    public DebtTotals debtTotals(Users user) {
        BigDecimal owes = BigDecimal.ZERO;
        BigDecimal owed = BigDecimal.ZERO;
        for (PairDebt debt : debtsOf(user).values()) {
            if (debt.amount.signum() > 0) owes = owes.add(debt.amount);
            else owed = owed.add(debt.amount.negate());
        }
        return new DebtTotals(owes, owed);
    }

    /**
     * Records that the user paid everything they currently owe the receiver. The payment is stored
     * per group, so each group's balances stay right even when it covered several groups.
     */
    @Transactional
    public void recordSettlement(RecordSettlementRequest request, String fromUserEmail) {
        Users fromUser = userRepo.findByEmail(fromUserEmail);
        Users toUser = userRepo.findById(request.getToUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found"));

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        PairDebt debt = debtsOf(fromUser).get(toUser.getId());
        // The app pays the amount it was shown; if anything changed since, ask it to reload first
        if (debt == null || debt.amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You don't owe " + toUser.getName() + " anything now. Refresh and try again.");
        }
        if (debt.amount.compareTo(request.getAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Balances have changed: you now owe " + toUser.getName() + " ₹" + debt.amount + ". Refresh and try again.");
        }

        for (Map.Entry<UUID, BigDecimal> part : debt.byGroupId.entrySet()) {
            Groups group = debt.groups.get(part.getKey());
            BigDecimal amount = part.getValue();
            if (amount.signum() > 0) {
                saveSettlement(fromUser, toUser, amount, request.getNote(), group);
            } else if (amount.signum() < 0) {
                // In this group the receiver owed the payer, which the single payment already netted off
                saveSettlement(toUser, fromUser, amount.negate(),
                        "Netted against " + fromUser.getName() + "'s payment of ₹" + request.getAmount(), group);
            }
        }
        debt.groups.values().forEach(this::clearIfSettled);

        // Trigger Push Notification to Receiver
        String token = toUser.getFcmToken();
        if (token != null && !token.trim().isEmpty()) {
            try {
                notificationService.sendPushNotification(
                        token,
                        "Payment Received",
                        fromUser.getName() + " settled up ₹" + request.getAmount()
                );
            } catch (Exception e) {
                // Log the error but don't fail the transaction if notification fails
                System.err.println("Notification failed: " + e.getMessage());
            }
        }
    }

    private void saveSettlement(Users from, Users to, BigDecimal amount, String note, Groups group) {
        Settlements settlement = new Settlements();
        settlement.setFromUser(from);
        settlement.setToUser(to);
        settlement.setAmount(amount);
        settlement.setNote(note);
        settlement.setGroup(group);
        settlementRepo.save(settlement);
    }

    /**
     * Once everyone in the group is square, marks its splits settled and stops counting its
     * payments. Until then splits stay open, because a simplified payment can't be pinned to
     * particular expenses.
     */
    @Transactional
    public void clearIfSettled(Groups group) {
        boolean everyoneSquare = groupBalances(group).values().stream().allMatch(b -> b.signum() == 0);
        if (!everyoneSquare) return;

        List<ExpenseSplits> openSplits = expenseSplitRepo.findByExpenseGroupAndSettledFalse(group);
        openSplits.forEach(split -> split.setSettled(true));
        expenseSplitRepo.saveAll(openSplits);

        List<Settlements> openPayments = settlementRepo.findOpenByGroup(group);
        openPayments.forEach(payment -> payment.setCleared(true));
        settlementRepo.saveAll(openPayments);
    }
}
