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
import java.util.List;
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

    public void addExpense(AddExpenseRequest request, String payerEmail) {

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

        // Round shares down; the payer absorbs the leftover paise so shares always add up to the amount
        BigDecimal equalShare =
                request.getAmount().divide(
                        BigDecimal.valueOf(memberCount),
                        2,
                        RoundingMode.DOWN
                );
        BigDecimal payerShare = request.getAmount()
                .subtract(equalShare.multiply(BigDecimal.valueOf(memberCount - 1)));


        Expenses expense = new Expenses();
        expense.setGroup(group);
        expense.setPayer(payer);
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());

        expenseRepo.save(expense);


        for (GroupMembers gm : members) {
            ExpenseSplits split = new ExpenseSplits();
            split.setExpense(expense);
            split.setUser(gm.getUser());
            if (gm.getUser().getId().equals(payer.getId())) {
                split.setShareAmount(payerShare);
                split.setSettled(true);   // payer owes nothing
            } else {
                split.setShareAmount(equalShare);
                split.setSettled(false);  // others owe payer
            }

            expenseSplitRepo.save(split);
        }

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
        System.out.println("expense Deleted");
    }

}
