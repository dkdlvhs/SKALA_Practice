package com.skala.shopapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.skala.shopapi.common.JwtTokenProvider;

class JwtTokenProviderTest {
    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("01234567890123456789012345678901", 3600);

    @Test
    void shouldCreateAndParseAccessTokenWithRole() {
        String token = jwtTokenProvider.createToken("admin", "ADMIN");

        JwtTokenProvider.TokenClaims claims = jwtTokenProvider.parseAndValidate(token);

        assertThat(claims).isNotNull();
        assertThat(claims.customerId()).isEqualTo("admin");
        assertThat(claims.role()).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.createToken("user", "USER");
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtTokenProvider.parseAndValidate(tampered)).isNull();
    }
}
