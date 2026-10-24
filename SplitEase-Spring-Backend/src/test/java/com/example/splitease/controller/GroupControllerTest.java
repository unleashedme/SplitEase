package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.CreateGroupRequest;
import com.example.splitease.requestAndResponse.GroupDataResponse;
import com.example.splitease.requestAndResponse.UserGroupResponse;
import com.example.splitease.service.GroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.example.splitease.controller.ControllerTestSupport.ALICE;
import static com.example.splitease.controller.ControllerTestSupport.mvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock private GroupService groupService;

    @InjectMocks private GroupController groupController;

    @Test
    void createGroupReturnsIdAndMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(groupService.createGroup(any(CreateGroupRequest.class), eq("alice@test.com"))).thenReturn(id);

        mvc(groupController).perform(post("/group").principal(ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\",\"members\":[\"bob@test.com\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(id.toString()))
                .andExpect(jsonPath("$.message").value("Group Created Successfully"));

        ArgumentCaptor<CreateGroupRequest> captor = ArgumentCaptor.forClass(CreateGroupRequest.class);
        verify(groupService).createGroup(captor.capture(), eq("alice@test.com"));
        assertThat(captor.getValue().getName()).isEqualTo("Trip");
        assertThat(captor.getValue().getMembers()).containsExactly("bob@test.com");
    }

    @Test
    void myGroupsListsMemberships() throws Exception {
        UUID id = UUID.randomUUID();
        when(groupService.getGroupsOfUser("alice@test.com"))
                .thenReturn(List.of(new UserGroupResponse(id, "Trip", "admin")));

        mvc(groupController).perform(get("/myGroups").principal(ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(id.toString()))
                .andExpect(jsonPath("$[0].groupName").value("Trip"))
                .andExpect(jsonPath("$[0].role").value("admin"));
    }

    @Test
    void groupScreenDataIsReturned() throws Exception {
        when(groupService.getGroupScreenData("alice@test.com"))
                .thenReturn(new GroupDataResponse(2, 3, new BigDecimal("390.00"), 1, List.of()));

        mvc(groupController).perform(get("/group").principal(ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGroups").value(2))
                .andExpect(jsonPath("$.totalMembersAcrossGroups").value(3))
                .andExpect(jsonPath("$.activeGroupsCount").value(1));
    }

    @Test
    void deleteGroupDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();

        mvc(groupController).perform(delete("/group/{id}", id).principal(ALICE))
                .andExpect(status().isOk());

        verify(groupService).deleteGroup(id, "alice@test.com");
    }
}
