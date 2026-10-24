package com.example.splitease.requestAndResponse;

import java.util.List;
import java.util.UUID;

public class UserGroupResponse {
    private UUID groupId;
    private String groupName;
    private String role;
    private List<GroupMemberDTO> members;

    public UserGroupResponse(UUID groupId, String groupName, String role) {
        this(groupId, groupName, role, List.of());
    }

    public UserGroupResponse(UUID groupId, String groupName, String role, List<GroupMemberDTO> members) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.role = role;
        this.members = members;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<GroupMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberDTO> members) {
        this.members = members;
    }
}
