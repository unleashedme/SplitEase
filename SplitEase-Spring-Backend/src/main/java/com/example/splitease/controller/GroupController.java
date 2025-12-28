package com.example.splitease.controller;


import com.example.splitease.requestAndResponse.CreateGroupRequest;
import com.example.splitease.requestAndResponse.GroupDataResponse;
import com.example.splitease.requestAndResponse.UserGroupResponse;
import com.example.splitease.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
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

    @GetMapping("/group")
    public ResponseEntity<GroupDataResponse> getGroupData(Principal principal){
        return ResponseEntity.ok(groupService.getGroupScreenData(principal.getName()));
    }

    @DeleteMapping("/group/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable("id") UUID id, Principal principal) throws AccessDeniedException {
        groupService.deleteGroup(id, principal.getName());
        return ResponseEntity.ok().build();
    }
}
