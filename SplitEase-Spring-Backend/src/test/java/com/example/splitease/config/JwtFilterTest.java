package com.example.splitease.config;

import com.example.splitease.models.UserPrincipal;
import com.example.splitease.service.JWTService;
import com.example.splitease.service.MyUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static com.example.splitease.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private JWTService jwtService;
    @Mock private ApplicationContext context;
    @Mock private MyUserDetailsService userDetailsService;

    @InjectMocks
    private JwtFilter jwtFilter;

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithAuth(String header) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dashboard");
        if (header != null) req.addHeader("Authorization", header);
        return req;
    }

    @Test
    void validTokenAuthenticatesRequest() throws Exception {
        UserPrincipal alice = new UserPrincipal(user("Alice", "alice@test.com"));
        when(jwtService.extractEmail("good")).thenReturn("alice@test.com");
        when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(alice);
        when(jwtService.validateToken("good", alice)).thenReturn(true);

        jwtFilter.doFilter(requestWithAuth("Bearer good"), response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isSameAs(alice);
        assertThat(auth.getAuthorities()).isNotEmpty();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void invalidTokenLeavesRequestUnauthenticated() throws Exception {
        UserPrincipal alice = new UserPrincipal(user("Alice", "alice@test.com"));
        when(jwtService.extractEmail("stale")).thenReturn("alice@test.com");
        when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(alice);
        when(jwtService.validateToken("stale", alice)).thenReturn(false);

        jwtFilter.doFilter(requestWithAuth("Bearer stale"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void missingHeaderPassesThrough() throws Exception {
        jwtFilter.doFilter(requestWithAuth(null), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, context);
    }

    @Test
    void nonBearerHeaderPassesThrough() throws Exception {
        jwtFilter.doFilter(requestWithAuth("Basic YTpi"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, context);
    }

    @Test
    void malformedTokenDoesNotBreakFilterChain() throws Exception {
        when(jwtService.extractEmail("garbage")).thenThrow(new MalformedJwtException("bad"));

        jwtFilter.doFilter(requestWithAuth("Bearer garbage"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void expiredTokenDoesNotBreakFilterChain() throws Exception {
        when(jwtService.extractEmail("old")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        jwtFilter.doFilter(requestWithAuth("Bearer old"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void tokenForDeletedUserDoesNotBreakFilterChain() throws Exception {
        when(jwtService.extractEmail("orphan")).thenReturn("gone@test.com");
        when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("gone@test.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtFilter.doFilter(requestWithAuth("Bearer orphan"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
