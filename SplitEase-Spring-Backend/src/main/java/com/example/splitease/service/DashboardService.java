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
    private SettlementService settlementService;

    public DashboardSummaryDTO getDashboardSummary(String userEmail) {

        Users user = userRepo.findByEmail(userEmail);

        // get all Groups the user belongs to
        List<Groups> groups = groupMemberRepo.findByUser(user)
                .stream()
                .map(GroupMembers::getGroup)
                .toList();

        if (groups.isEmpty()) return new DashboardSummaryDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // Same simplified debts as the Settle Up screen, so the two always agree
        SettlementService.DebtTotals debts = settlementService.debtTotals(user);
        BigDecimal owes = debts.owes();
        BigDecimal owedTo = debts.owed();

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
