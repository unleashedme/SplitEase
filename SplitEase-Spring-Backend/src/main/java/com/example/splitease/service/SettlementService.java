package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.RecordSettlementRequest;
import com.example.splitease.requestAndResponse.SettlementSummaryResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SettlementService {

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private SettlementRepo settlementRepo;

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;


    public List<SettlementSummaryResponse> getMyPayables(String userEmail) {
        Users currentUser = userRepo.findByEmail(userEmail);

        // 1. Use a single optimized query to get net balances with everyone
        // You can also write a custom native query if JPA logic gets too complex
        List<Object[]> rawBalances = expenseSplitRepo.calculateNetBalances(currentUser.getId());

        List<SettlementSummaryResponse> payables = new ArrayList<>();

        for (Object[] row : rawBalances) {
            String otherUserId = row[0].toString();
            String otherUserName = (String) row[1];
            BigDecimal balance = (BigDecimal) row[2];

            // If balance > 0, I owe them. If < 0, they owe me.
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                payables.add(new SettlementSummaryResponse(
                        otherUserId,
                        otherUserName,
                        balance
                ));
            }
        }
        return payables;
    }


    @Transactional
    public void recordSettlement(RecordSettlementRequest request, String fromUserEmail) {
        // 1. Fetch Entities
        Users fromUser = userRepo.findByEmail(fromUserEmail);
        Users toUser = userRepo.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));


        // 2. Create and Save Settlement record (Audit Trail)
        Settlements settlement = new Settlements();
        settlement.setFromUser(fromUser);
        settlement.setToUser(toUser);
        settlement.setAmount(request.getAmount());
        settlement.setNote(request.getNote());
        settlementRepo.save(settlement);

        // 3. Mark ALL splits between these two users as settled (Both Directions)
        // We fetch splits where From owes To or To owes From
        List<ExpenseSplits> allRelatedSplits = expenseSplitRepo.findAllUnsettledBetweenUsers(
                fromUser.getId(),
                toUser.getId()
        );

        for (ExpenseSplits split : allRelatedSplits) {
            split.setSettled(true);
        }
        expenseSplitRepo.saveAll(allRelatedSplits);



        // 4. Trigger Push Notification to Receiver
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
}
