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


    public List<SettlementSummaryResponse> getMyPayables(String userEmail) {
        Users currentUser = userRepo.findByEmail(userEmail);

        List<SettlementSummaryResponse> allPayables = new ArrayList<>();

        // find all groups this user is in
        List<GroupMembers> userMemberships = groupMemberRepo.findByUserEmail(userEmail);

        for (GroupMembers membership : userMemberships) {
            Groups currentGroup = membership.getGroup();

            // find all other members in this group
            List<GroupMembers> allGroupMembers = groupMemberRepo.findByGroupId(currentGroup.getId());

            for (GroupMembers otherMemberEntry : allGroupMembers) {
                Users otherUser = otherMemberEntry.getUser();

                // Skip if comparing user to themselves
                if (otherUser.getId().equals(currentUser.getId())) continue;

                // calculate how much I owe them in this group (Unsettled only)
                BigDecimal iOweThem = expenseSplitRepo.findTotalOwedByTo(
                        currentUser.getId(), otherUser.getId(), currentGroup.getId());
                iOweThem = (iOweThem != null) ? iOweThem : BigDecimal.ZERO;

                // calculate how much they owe me in this group (Unsettled only)
                BigDecimal theyOweMe = expenseSplitRepo.findTotalOwedByTo(
                        otherUser.getId(), currentUser.getId(), currentGroup.getId());
                theyOweMe = (theyOweMe != null) ? theyOweMe : BigDecimal.ZERO;

                // net calculation
                BigDecimal netBalance = iOweThem.subtract(theyOweMe);

                // if positive, it's a payable for the current user
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
        // fetch the entities
        Users fromUser = userRepo.findByEmail(fromUserEmail);
        Users toUser = userRepo.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        Groups group = groupRepo.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // create and save the Settlement record
        Settlements settlement = new Settlements();
        settlement.setFromUser(fromUser);
        settlement.setToUser(toUser);
        settlement.setGroup(group);
        settlement.setAmount(request.getAmount());
        settlement.setNote(request.getNote());
        settlementRepo.save(settlement);

        // mark related splits as settled (true), so they won't appear in getMyPayables next time
        // find all splits where 'fromUser' owed 'toUser' in this specific group
        List<ExpenseSplits> unsettledSplits = expenseSplitRepo
                .findByUserAndExpense_PayerAndExpense_GroupAndSettledFalse(fromUser, toUser, group);

        for (ExpenseSplits split : unsettledSplits) {
            split.setSettled(true);
        }

        expenseSplitRepo.saveAll(unsettledSplits);
    }
}
