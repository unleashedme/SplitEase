package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.*;
import com.example.splitease.requestAndResponse.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.AccessDeniedException;
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
    private ExpenseSplitRepo expenseSplitRepo;

    @Autowired
    private NotificationService notificationService;

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

            // Send notification
            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty() && !Objects.equals(user.getEmail(), creatorEmail)) {
                notificationService.sendPushNotification(
                        user.getFcmToken(),
                        "\uD83C\uDF1F New Group Created!",
                        creator.getName() + " added you to the group: " + request.getName()
                );
            }
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


            BigDecimal totalGrpExp = groupExpenses.stream().map(Expenses::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            grandTotalExpense = grandTotalExpense.add(totalGrpExp);
            allMembers.forEach(m -> uniqueUsers.add(m.getUser().getId()));

            groupDetails.add(new GroupDetailResponse(
                    group.getId(), group.getName(), isActive, totalGrpExp,
                    groupExpenses.size(), allMembers.size(), userShare,
                    allMembers.stream().map(m -> m.getUser().getName()).toList(),
                    expenseDTOs
            ));
        }

        return new GroupDataResponse(
                myGroups.size(), uniqueUsers.size(), grandTotalExpense, activeGroupsCount, groupDetails
        );
    }

    @Transactional
    public void deleteGroup(UUID groupId, String userEmail) throws AccessDeniedException {
        // 1. Fetch group or throw custom exception
        Groups group = groupRepo.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // 2. Security Check: Only the creator (Admin) can delete the group
        if (!group.getCreatedBy().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Only the group creator can delete this group.");
        }

        // 3. Perform the deletion
        // Since you added cascade = CascadeType.ALL to 'members',
        // deleting the group will automatically remove GroupMembers entries.
        groupRepo.delete(group);
    }
}
