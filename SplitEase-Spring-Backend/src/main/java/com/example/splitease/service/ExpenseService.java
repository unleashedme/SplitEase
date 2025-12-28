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

        Users payer = userRepo.findByEmail(payerEmail);

        Groups group = groupRepo.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<GroupMembers> members = groupMemberRepo.findByGroupId(group.getId());

        int memberCount = members.size();
        if (memberCount == 0) {
            throw new RuntimeException("Group has no members");
        }

        BigDecimal equalShare =
                request.getAmount().divide(
                        BigDecimal.valueOf(memberCount),
                        2,
                        RoundingMode.HALF_UP
                );


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
            split.setShareAmount(equalShare);
            if (gm.getUser().getId().equals(payer.getId())) {
                split.setSettled(true);   // payer owes nothing
            } else {
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
