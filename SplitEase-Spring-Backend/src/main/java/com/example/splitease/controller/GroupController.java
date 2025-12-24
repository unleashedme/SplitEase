package com.example.splitease.controller;


import com.example.splitease.requestAndResponse.CreateGroupRequest;
import com.example.splitease.requestAndResponse.UserGroupResponse;
import com.example.splitease.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request, Principal principal){

        System.out.println("RAW MEMBERS = " + request.getMembers());
        UUID groupId = groupService.createGroup(request, principal.getName());


        return ResponseEntity.ok(Map.of(
                "groupId", groupId,
                "message", "Group Created Successfully"
        ));
    }

    @GetMapping("/myGroups")
    public List<UserGroupResponse> getMyGroups(Principal principal) {
        return groupService.getGroupsOfUser(principal.getName());
    }
}
