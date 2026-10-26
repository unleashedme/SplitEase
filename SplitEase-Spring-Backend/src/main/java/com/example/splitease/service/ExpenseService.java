package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.AddExpenseRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseService {

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SettlementService settlementService;

    public UUID addExpense(AddExpenseRequest request, String payerEmail) {

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        Users payer = userRepo.findByEmail(payerEmail);

        Groups group = groupRepo.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<GroupMembers> members = groupMemberRepo.findByGroupId(group.getId());

        int memberCount = members.size();
        if (memberCount == 0) {
            throw new RuntimeException("Group has no members");
        }

        // Only members of the group can add expenses to it
        boolean payerIsMember = members.stream()
                .anyMatch(gm -> gm.getUser().getId().equals(payer.getId()));
        if (!payerIsMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        SplitType splitType = request.getSplitType() == null ? SplitType.EQUAL : request.getSplitType();
        Map<Users, BigDecimal> shares = calculateShares(request.getAmount(), splitType, request.getSplits(), members, payer);


        Expenses expense = new Expenses();
        expense.setGroup(group);
        expense.setPayer(payer);
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setSplitType(splitType);

        expenseRepo.save(expense);


        for (Map.Entry<Users, BigDecimal> entry : shares.entrySet()) {
            ExpenseSplits split = new ExpenseSplits();
            split.setExpense(expense);
            split.setUser(entry.getKey());
            split.setShareAmount(entry.getValue());
            // payer owes nothing; others owe payer
            split.setSettled(entry.getKey().getId().equals(payer.getId()));

            expenseSplitRepo.save(split);
        }
        settlementService.clearIfSettled(group);

        for (GroupMembers member : members) {
            Users userToNotify = member.getUser();

            // don't send a notification to the person who just added the expense
            if (!userToNotify.getId().equals(expense.getPayer().getId())) {

                String token = userToNotify.getFcmToken();
                if (token != null && !token.isEmpty()) {
                    notificationService.sendPushNotification(
                            token,
                            "New Expense in " + expense.getGroup().getName(),
                            expense.getPayer().getName() + " added ₹" + expense.getAmount() + " for " + expense.getDescription()
                    );
                }
            }
        }

        return expense.getId();
    }

    /**
     * Works out how much each participant owes, in group-member order.
     * Members left out of the split (or given a zero value) get no split row.
     */
    private Map<Users, BigDecimal> calculateShares(BigDecimal amount, SplitType splitType,
                                                   List<AddExpenseRequest.SplitEntry> entries,
                                                   List<GroupMembers> members, Users payer) {

        // No split details means the old behaviour: everyone in the group shares equally
        if (entries == null || entries.isEmpty()) {
            if (splitType != SplitType.EQUAL) {
                throw badRequest("Split details are required for a " + splitType + " split");
            }
            entries = members.stream()
                    .map(gm -> new AddExpenseRequest.SplitEntry(gm.getUser().getId(), null))
                    .toList();
        }

        Map<UUID, BigDecimal> valueByUserId = new HashMap<>();
        for (AddExpenseRequest.SplitEntry entry : entries) {
            if (entry.getUserId() == null) {
                throw badRequest("Every split needs a userId");
            }
            boolean isMember = members.stream().anyMatch(gm -> gm.getUser().getId().equals(entry.getUserId()));
            if (!isMember) {
                throw badRequest("User " + entry.getUserId() + " is not a member of this group");
            }

            // EQUAL ignores values: everyone listed takes one equal part
            BigDecimal value = splitType == SplitType.EQUAL ? BigDecimal.ONE : entry.getValue();
            if (value == null || value.signum() < 0) {
                throw badRequest("Split values must be zero or more");
            }
            if (valueByUserId.put(entry.getUserId(), value) != null) {
                throw badRequest("A member appears more than once in the split");
            }
        }

        // Keep group-member order and drop anyone who isn't taking part
        Map<Users, BigDecimal> weights = new LinkedHashMap<>();
        for (GroupMembers gm : members) {
            BigDecimal value = valueByUserId.get(gm.getUser().getId());
            if (value != null && value.signum() > 0) {
                weights.put(gm.getUser(), value);
            }
        }
        if (weights.isEmpty()) {
            throw badRequest("At least one member must share the expense");
        }

        BigDecimal totalWeight = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        switch (splitType) {
            case EXACT -> {
                if (weights.values().stream().anyMatch(v -> v.stripTrailingZeros().scale() > 2)) {
                    throw badRequest("Split amounts can have at most 2 decimal places");
                }
                if (totalWeight.compareTo(amount) != 0) {
                    throw badRequest("Split amounts add up to " + totalWeight + " but the expense is " + amount);
                }
                return weights;
            }
            case PERCENTAGE -> {
                if (totalWeight.compareTo(BigDecimal.valueOf(100)) != 0) {
                    throw badRequest("Percentages add up to " + totalWeight + " but must add up to 100");
                }
            }
            default -> { }
        }

        // Proportional split: round each share down, then hand the leftover paise to the payer
        // (or the first participant if the payer isn't sharing) so shares always add up to the amount
        Map<Users, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (Map.Entry<Users, BigDecimal> entry : weights.entrySet()) {
            BigDecimal share = amount.multiply(entry.getValue()).divide(totalWeight, 2, RoundingMode.DOWN);
            shares.put(entry.getKey(), share);
            allocated = allocated.add(share);
        }

        Users absorber = shares.keySet().stream()
                .filter(u -> u.getId().equals(payer.getId()))
                .findFirst()
                .orElse(shares.keySet().iterator().next());
        shares.merge(absorber, amount.subtract(allocated), BigDecimal::add);

        return shares;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @Transactional
    public void deleteExpense(UUID expenseId, String userEmail) throws AccessDeniedException {
        System.out.println("delete expense called");
        Expenses expense = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));

        // If User is not payer then do not delete
        if (!expense.getPayer().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Unauthorized");
        }

        expenseRepo.delete(expense); // splits will be deleted automatically
        settlementService.clearIfSettled(expense.getGroup());
        System.out.println("expense Deleted");
    }

}
