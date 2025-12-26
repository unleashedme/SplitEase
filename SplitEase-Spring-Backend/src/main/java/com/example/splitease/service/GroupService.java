package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class GroupService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private SettlementRepo settlementRepo;

    @Autowired
    private ExpenseSplitRepo expenseSplitRepo;

    public UUID createGroup(CreateGroupRequest request, String creatorEmail){

        Users creator = userRepo.findByEmail(creatorEmail);

        Set<String> emails = new HashSet<>(request.getMembers());
        emails.add(creatorEmail);

        if(emails.size()>5){
            throw new RuntimeException("Group can have maximum 10 members");
        }

        List<Users> users = emails.stream()
                .map(email -> userRepo.findByEmail(email)).toList();

        Groups group = new Groups();
        group.setName(request.getName());
        group.setCreatedBy(creator);

        groupRepo.save(group);

        for(Users user : users){
            GroupMembers groupMember = new GroupMembers();
            groupMember.setUser(user);
            groupMember.setGroup(group);
            groupMember.setRole(user.getEmail().equals(creatorEmail) ? "admin" : "member");
            groupMemberRepo.save(groupMember);
        }

        return group.getId();
    }

    public List<UserGroupResponse> getGroupsOfUser(String email) {

        return groupMemberRepo.findAllByUserEmail(email)
                .stream()
                .map(gm -> new UserGroupResponse(
                        gm.getGroup().getId(),
                        gm.getGroup().getName(),
                        gm.getRole()
                ))
                .toList();
    }

    public GroupDataResponse getGroupScreenData(String userEmail) {
        // 1. Get all GroupMemberships for the current user
        List<GroupMembers> memberships = groupMemberRepo.findByUserEmail(userEmail);
        List<Groups> myGroups = memberships.stream().map(GroupMembers::getGroup).toList();

        List<GroupDetailResponse> groupDetails = new ArrayList<>();
        BigDecimal grandTotalExpense = BigDecimal.ZERO;
        Set<UUID> uniqueUsers = new HashSet<>();
        long activeGroupsCount = 0;

        for (Groups group : myGroups) {
            // Fetch Expenses and Settlements for this specific group
            List<Expenses> groupExpenses = expenseRepo.findByGroup(group);
            List<Settlements> groupSettlements = settlementRepo.findByGroup(group);
            List<GroupMembers> allMembers = groupMemberRepo.findByGroup(group);

            // Calculate Active Status (If any split in this group is not settled)
            boolean isActive = expenseSplitRepo.existsByExpenseGroupAndSettledFalse(group);
            if (isActive) activeGroupsCount++;

            // Calculate User's specific share in this group
            BigDecimal userShare = expenseSplitRepo.findByExpenseGroupAndUserEmail(group, userEmail)
                    .stream().map(ExpenseSplits::getShareAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Map Expenses
            List<ExpenseDTO> expenseDTOs = groupExpenses.stream().map(e -> {
                BigDecimal splitCount = new BigDecimal(allMembers.size());
                return new ExpenseDTO(
                        e.getDescription(),
                        e.getAmount(),
                        e.getPayer().getName(),
                        e.getPayer().getEmail().equals(userEmail),
                        e.getCreatedAt().toString(),
                        e.getAmount().divide(splitCount, 2, RoundingMode.HALF_UP)
                );
            }).toList();

            // Map Settlements
            List<SettlementDTO> settlementDTOs = groupSettlements.stream().map(s ->
                    new SettlementDTO(s.getFromUser().getName(), s.getToUser().getName(), s.getAmount(), s.getCreatedAt().toString(), s.getNote())
            ).toList();

            BigDecimal totalGrpExp = groupExpenses.stream().map(Expenses::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            grandTotalExpense = grandTotalExpense.add(totalGrpExp);
            allMembers.forEach(m -> uniqueUsers.add(m.getUser().getId()));

            groupDetails.add(new GroupDetailResponse(
                    group.getId(), group.getName(), isActive, totalGrpExp,
                    groupExpenses.size(), allMembers.size(), userShare,
                    allMembers.stream().map(m -> m.getUser().getName()).toList(),
                    settlementDTOs, expenseDTOs
            ));
        }

        return new GroupDataResponse(
                myGroups.size(), uniqueUsers.size(), grandTotalExpense, activeGroupsCount, groupDetails
        );
    }

}
