package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.LogInCredentials;
import com.example.splitease.requestAndResponse.LogInResponse;
import com.example.splitease.requestAndResponse.RegisterUserDetails;
import com.example.splitease.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import static com.example.splitease.controller.ControllerTestSupport.ALICE;
import static com.example.splitease.controller.ControllerTestSupport.mvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;

    @InjectMocks private UserController userController;

    @Test
    void registerBindsAllFields() throws Exception {
        mvc(userController).perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"alice@test.com\",\"phone\":\"999\",\"password\":\"pw\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<RegisterUserDetails> captor = ArgumentCaptor.forClass(RegisterUserDetails.class);
        verify(userService).register(captor.capture());
        RegisterUserDetails r = captor.getValue();
        assertThat(r.getName()).isEqualTo("Alice");
        assertThat(r.getEmail()).isEqualTo("alice@test.com");
        assertThat(r.getPhone()).isEqualTo("999");
        assertThat(r.getPassword()).isEqualTo("pw");
    }

    @Test
    void loginBindsCredentialsAndReturnsToken() throws Exception {
        when(userService.verify(any(LogInCredentials.class)))
                .thenReturn(new LogInResponse("jwt", "Alice", "alice@test.com", "999"));

        mvc(userController).perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@test.com\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.phone").value("999"));

        ArgumentCaptor<LogInCredentials> captor = ArgumentCaptor.forClass(LogInCredentials.class);
        verify(userService).verify(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@test.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("pw");
    }

    @Test
    void updateTokenStoresTokenForPrincipal() throws Exception {
        mvc(userController).perform(put("/update-token").param("token", "fcm-123").principal(ALICE))
                .andExpect(status().isOk());

        verify(userService).updateUserFcmToken("alice@test.com", "fcm-123");
    }
}
