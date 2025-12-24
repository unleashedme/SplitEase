package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.RecordSettlementRequest;
import com.example.splitease.requestAndResponse.SettlementSummaryResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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


    public List<SettlementSummaryResponse> getMyPayables(String userEmail) {
        Users currentUser = userRepo.findByEmail(userEmail);

        List<SettlementSummaryResponse> allPayables = new ArrayList<>();

        // 1. Find all groups this user is in
        List<GroupMembers> userMemberships = groupMemberRepo.findByUserEmail(userEmail);

        for (GroupMembers membership : userMemberships) {
            Groups currentGroup = membership.getGroup();

            // 2. Find all other members in THIS group
            List<GroupMembers> allGroupMembers = groupMemberRepo.findByGroupId(currentGroup.getId());

            for (GroupMembers otherMemberEntry : allGroupMembers) {
                Users otherUser = otherMemberEntry.getUser();

                // Skip if comparing user to themselves
                if (otherUser.getId().equals(currentUser.getId())) continue;

                // 3. Calculate how much I owe them in this group (Unsettled only)
                BigDecimal iOweThem = expenseSplitRepo.findTotalOwedByTo(
                        currentUser.getId(), otherUser.getId(), currentGroup.getId());
                iOweThem = (iOweThem != null) ? iOweThem : BigDecimal.ZERO;

                // 4. Calculate how much they owe me in this group (Unsettled only)
                BigDecimal theyOweMe = expenseSplitRepo.findTotalOwedByTo(
                        otherUser.getId(), currentUser.getId(), currentGroup.getId());
                theyOweMe = (theyOweMe != null) ? theyOweMe : BigDecimal.ZERO;

                // 5. Net calculation
                BigDecimal netBalance = iOweThem.subtract(theyOweMe);

                // If positive, it's a payable for the current user
                if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
                    allPayables.add(new SettlementSummaryResponse(
                            otherUser.getId(),
                            otherUser.getName(),
                            netBalance,
                            currentGroup.getId(),
                            currentGroup.getName()
                    ));
                }
            }
        }
        return allPayables;
    }


    @Transactional
    public void recordSettlement(RecordSettlementRequest request, String fromUserEmail) {
        // 1. Fetch the entities
        Users fromUser = userRepo.findByEmail(fromUserEmail);
        Users toUser = userRepo.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        Groups group = groupRepo.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // 2. Create and Save the Settlement record (The Audit Trail)
        Settlements settlement = new Settlements();
        settlement.setFromUser(fromUser);
        settlement.setToUser(toUser);
        settlement.setGroup(group);
        settlement.setAmount(request.getAmount());
        settlement.setNote(request.getNote());
        settlementRepo.save(settlement);

        // 3. THE CRITICAL STEP: Mark related splits as settled (true)
        // Find all splits where 'fromUser' owed 'toUser' in this specific group
        List<ExpenseSplits> unsettledSplits = expenseSplitRepo
                .findByUserAndExpense_PayerAndExpense_GroupAndSettledFalse(fromUser, toUser, group);

        for (ExpenseSplits split : unsettledSplits) {
            split.setSettled(true); // Now marked true, so they won't appear in getMyPayables next time
        }

        expenseSplitRepo.saveAll(unsettledSplits);
    }
}
