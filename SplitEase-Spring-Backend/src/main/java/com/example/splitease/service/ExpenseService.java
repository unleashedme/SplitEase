package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.AddExpenseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
    }

}
