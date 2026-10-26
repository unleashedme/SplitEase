package com.example.splitease.service;

import com.example.splitease.models.ExpenseSplits;
import com.example.splitease.models.Expenses;
import com.example.splitease.models.SplitType;
import com.example.splitease.repo.ExpenseReceiptRepo;
import com.example.splitease.repo.ExpenseRepo;
import com.example.splitease.repo.ExpenseSplitRepo;
import com.example.splitease.requestAndResponse.ActivityDashboardResponse;
import com.example.splitease.requestAndResponse.ExpenseActivityDTO;
import com.example.splitease.requestAndResponse.SplitShareDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ActivityService {

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private ExpenseReceiptRepo expenseReceiptRepo;

    public ActivityDashboardResponse getUserActivity(String email) {
        // 1. Fetch only expenses where the user is a Group Member AND (Payer OR Participant)
        List<Expenses> expenses = expenseRepo.findAllActivityForUser(email);

        long totalExpensesCount = expenses.size();
        BigDecimal totalSpent = BigDecimal.ZERO;
        long paidCount = 0;
        long settledCount = 0;
        List<ExpenseActivityDTO> history = new ArrayList<>();
        Set<UUID> expensesWithReceipt = expenses.isEmpty() ? Set.of()
                : expenseReceiptRepo.findExpenseIdsWithReceipt(expenses.stream().map(Expenses::getId).toList());

        for (Expenses exp : expenses) {
            boolean isPayer = exp.getPayer().getEmail().equals(email);

            // 2. Find the current user's specific split in this expense
            // This is safe because the query ensured the user is involved
            ExpenseSplits mySplit = exp.getSplits().stream()
                    .filter(s -> s.getUser().getEmail().equals(email))
                    .findFirst()
                    .orElse(null);

            BigDecimal userShare = (mySplit != null) ? mySplit.getShareAmount() : BigDecimal.ZERO;

            // 3. New Settled Logic:
            // Payer is settled if everyone else paid. Participant is settled if their split is true.
            boolean everyoneElsePaid = exp.getSplits().stream()
                    .allMatch(s -> Boolean.TRUE.equals(s.getSettled()));

            boolean isSettledForUser = isPayer ? everyoneElsePaid : (mySplit != null && Boolean.TRUE.equals(mySplit.getSettled()));

            // 4. Update Stats
            if (isSettledForUser) settledCount++;
            if (isPayer) paidCount++;
            totalSpent = totalSpent.add(userShare);

            // 5. Determine "Owes To"
            String owesTo = (!isPayer && mySplit != null && !Boolean.TRUE.equals(mySplit.getSettled()))
                    ? exp.getPayer().getName() : null;

            history.add(new ExpenseActivityDTO(
                    exp.getId(),
                    exp.getDescription(),
                    exp.getCreatedAt().toString(),
                    isPayer,
                    exp.getGroup().getName(),
                    exp.getAmount(),
                    userShare,
                    isSettledForUser,
                    owesTo,
                    expensesWithReceipt.contains(exp.getId()),
                    exp.getPayer().getName(),
                    (exp.getSplitType() == null ? SplitType.EQUAL : exp.getSplitType()).name(),
                    shares(exp, email)
            ));
        }

        return new ActivityDashboardResponse(totalExpensesCount, totalSpent, paidCount, settledCount, history);
    }

    // Payer first, then everyone else by name
    private static List<SplitShareDTO> shares(Expenses exp, String email) {
        return exp.getSplits().stream()
                .sorted(Comparator
                        .comparing((ExpenseSplits s) -> !s.getUser().getId().equals(exp.getPayer().getId()))
                        .thenComparing(s -> s.getUser().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(s -> new SplitShareDTO(
                        s.getUser().getName(),
                        s.getShareAmount(),
                        s.getUser().getId().equals(exp.getPayer().getId()),
                        s.getUser().getEmail().equals(email),
                        Boolean.TRUE.equals(s.getSettled())
                ))
                .toList();
    }

}
