package com.skala.shopapi.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtTokenProvider {
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds:3600}") long expirationSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret은 32바이트 이상이어야 합니다.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(String customerId, String role) {
        try {
            long issuedAt = Instant.now().getEpochSecond();
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", customerId);
            payload.put("role", role);
            payload.put("iat", issuedAt);
            payload.put("exp", issuedAt + expirationSeconds);

            String unsignedToken = encode(objectMapper.writeValueAsBytes(header))
                    + "." + encode(objectMapper.writeValueAsBytes(payload));
            return unsignedToken + "." + encode(sign(unsignedToken));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT 발급에 실패했습니다.", ex);
        }
    }

    public TokenClaims parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = BASE64_URL_DECODER.decode(parts[2]);
            if (!java.security.MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return null;
            }

            Map<String, Object> claims = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]), new TypeReference<>() {});
            String customerId = (String) claims.get("sub");
            String role = (String) claims.get("role");
            long expiration = ((Number) claims.get("exp")).longValue();
            if (customerId == null || role == null || expiration <= Instant.now().getEpochSecond()) {
                return null;
            }
            return new TokenClaims(customerId, role, expiration);
        } catch (Exception ex) {
            return null;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String encode(byte[] value) {
        return BASE64_URL_ENCODER.encodeToString(value);
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    public record TokenClaims(String customerId, String role, long expiration) {
    }
}
