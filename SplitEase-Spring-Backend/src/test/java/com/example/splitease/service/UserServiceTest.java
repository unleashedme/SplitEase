package com.example.splitease.service;

import com.example.splitease.models.Users;
import com.example.splitease.repo.UserRepo;
import com.example.splitease.requestAndResponse.LogInCredentials;
import com.example.splitease.requestAndResponse.LogInResponse;
import com.example.splitease.requestAndResponse.RegisterUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static com.example.splitease.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private AuthenticationManager authManager;
    @Mock private JWTService jwtService;

    @InjectMocks
    private UserService userService;

    private RegisterUserDetails registration() {
        RegisterUserDetails r = new RegisterUserDetails();
        r.setName("Alice");
        r.setEmail("alice@test.com");
        r.setPhone("9999999999");
        r.setPassword("secret");
        return r;
    }

    private LogInCredentials credentials(String email, String password) {
        LogInCredentials c = new LogInCredentials();
        ReflectionTestUtils.setField(c, "email", email);
        ReflectionTestUtils.setField(c, "password", password);
        return c;
    }

    @Test
    void registerSavesUserWithHashedPassword() {
        userService.register(registration());

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepo).save(captor.capture());
        Users saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getEmail()).isEqualTo("alice@test.com");
        assertThat(saved.getPhone()).isEqualTo("9999999999");
        assertThat(saved.getPassword()).isNotEqualTo("secret");
        assertThat(new BCryptPasswordEncoder().matches("secret", saved.getPassword())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepo.findByEmail("alice@test.com")).thenReturn(user("Alice", "alice@test.com"));

        assertThatThrownBy(() -> userService.register(registration()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(userRepo, never()).save(any());
    }

    @Test
    void verifyReturnsTokenAndUpdatesLastActive() {
        Users alice = user("Alice", "alice@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(authManager.authenticate(any())).thenReturn(auth);
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);
        when(jwtService.generateToken("alice@test.com")).thenReturn("jwt-token");
        Instant before = Instant.now();

        LogInResponse res = userService.verify(credentials("alice@test.com", "secret"));

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getName()).isEqualTo("Alice");
        assertThat(res.getEmail()).isEqualTo("alice@test.com");
        assertThat(res.getPhone()).isEqualTo(alice.getPhone());
        assertThat(alice.getLastActiveAt()).isAfterOrEqualTo(before);
        verify(userRepo).save(alice);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("alice@test.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("secret");
    }

    @Test
    void verifyPropagatesBadCredentials() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> userService.verify(credentials("alice@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void verifyRejectsUnauthenticatedResult() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        when(authManager.authenticate(any())).thenReturn(auth);

        assertThatThrownBy(() -> userService.verify(credentials("alice@test.com", "secret")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void updateUserFcmTokenStoresToken() {
        Users alice = user("Alice", "alice@test.com");
        when(userRepo.findByEmail("alice@test.com")).thenReturn(alice);

        userService.updateUserFcmToken("alice@test.com", "new-token");

        assertThat(alice.getFcmToken()).isEqualTo("new-token");
        verify(userRepo).save(alice);
    }
}
