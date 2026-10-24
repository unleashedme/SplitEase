package com.example.splitease.service;

import com.example.splitease.models.UserPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static com.example.splitease.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTServiceTest {

    static final String SECRET = "c3BsaXRlYXNlLXRlc3Qtc2VjcmV0LWtleS0xMjM0NTY3ODkwLWFiY2RlZg==";

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
    }

    private UserDetails principal(String email) {
        return new UserPrincipal(user("X", email));
    }

    @Test
    void generatedTokenCarriesEmailAsSubject() {
        String token = jwtService.generateToken("alice@test.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@test.com");
    }

    @Test
    void tokenIsValidForSameUser() {
        String token = jwtService.generateToken("alice@test.com");

        assertThat(jwtService.validateToken(token, principal("alice@test.com"))).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentUser() {
        String token = jwtService.generateToken("alice@test.com");

        assertThat(jwtService.validateToken(token, principal("bob@test.com"))).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("alice@test.com");
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        assertThatThrownBy(() -> jwtService.extractEmail(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JWTService other = new JWTService();
        ReflectionTestUtils.setField(other, "secretKey",
                "YW5vdGhlci1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtMTIzNDU2");
        String foreign = other.generateToken("alice@test.com");

        assertThatThrownBy(() -> jwtService.extractEmail(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = Jwts.builder()
                .subject("alice@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        assertThatThrownBy(() -> jwtService.validateToken(expired, principal("alice@test.com")))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
