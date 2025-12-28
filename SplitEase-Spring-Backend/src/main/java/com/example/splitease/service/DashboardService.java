package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.DashboardSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class DashboardService {

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private GroupRepo groupRepository;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private ExpenseRepo expenseRepo;

    public DashboardSummaryDTO getDashboardSummary(String userEmail) {

        Users user = userRepo.findByEmail(userEmail);

        // get all Groups the user belongs to
        List<Groups> groups = groupMemberRepo.findByUser(user)
                .stream()
                .map(GroupMembers::getGroup)
                .toList();

        if (groups.isEmpty()) return null;

        // 2. Expenses of those groups
        List<Expenses> expenses = expenseRepo.findByGroupIn(groups);

        BigDecimal owes = BigDecimal.ZERO;
        BigDecimal owedTo = BigDecimal.ZERO;

        // 3. Process expenses
        for (Expenses expense : expenses) {

            boolean userIsPayer =
                    expense.getPayer().getId().equals(user.getId());

            for (ExpenseSplits split : expense.getSplits()) {

                if (Boolean.TRUE.equals(split.getSettled())) continue;

                // User paid → others owe user
                if (userIsPayer &&
                        !split.getUser().getId().equals(user.getId())) {

                    owedTo = owedTo.add(split.getShareAmount());
                }

                // User didn't pay → user owes
                if (!userIsPayer &&
                        split.getUser().getId().equals(user.getId())) {

                    owes = owes.add(split.getShareAmount());
                }
            }
        }

        // calculate Total Expenses (Lifetime consumption from splits)
        List<ExpenseSplits> userSplits = expenseSplitRepo.findAllByUserEmail(userEmail);
        BigDecimal totalExpenses = userSplits.stream()
                .map(ExpenseSplits::getShareAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        return new DashboardSummaryDTO(
                totalExpenses,
                owes,
                owedTo
        );
    }
}
