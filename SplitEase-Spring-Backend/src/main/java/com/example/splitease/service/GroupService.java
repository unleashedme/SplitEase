package com.example.splitease.service;

import com.example.splitease.requestAndResponse.CreateGroupRequest;
import com.example.splitease.models.GroupMembers;
import com.example.splitease.models.Groups;
import com.example.splitease.models.Users;
import com.example.splitease.repo.GroupMemberRepo;
import com.example.splitease.repo.GroupRepo;
import com.example.splitease.repo.UserRepo;
import com.example.splitease.requestAndResponse.UserGroupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

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

}
