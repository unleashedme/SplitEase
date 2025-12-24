package com.example.splitease.requestAndResponse;

import java.util.UUID;

public class UserGroupResponse {
    private UUID groupId;
    private String groupName;
    private String role;

    public UserGroupResponse(UUID groupId, String groupName, String role) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.role = role;
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
}
