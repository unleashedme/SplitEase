package com.example.splitease.service;

import com.example.splitease.models.Users;
import com.example.splitease.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static com.example.splitease.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @Mock
    private UserRepo repo;

    @InjectMocks
    private MyUserDetailsService service;

    @Test
    void loadsExistingUserAsPrincipal() {
        Users alice = user("Alice", "alice@test.com");
        when(repo.findByEmail("alice@test.com")).thenReturn(alice);

        UserDetails details = service.loadUserByUsername("alice@test.com");

        assertThat(details.getUsername()).isEqualTo("alice@test.com");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("USER");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void unknownUserThrows() {
        when(repo.findByEmail("ghost@test.com")).thenReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
