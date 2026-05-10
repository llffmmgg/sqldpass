package com.sqldpass.service.auth;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Google OIDC ID token 검증 — Google Pub/Sub push subscription 이 보낸
 * Authorization Bearer 헤더의 JWT 를 tokeninfo 엔드포인트로 검증한다.
 *
 * <p>{@link GoogleOAuthClient#verifyIdToken} 와 분리한 이유: 그쪽은 사용자 OAuth aud
 * (web/Android client id) 를 기대 — RTDN 의 aud (운영자가 Pub/Sub subscription 에 등록한
 * audience, 보통 endpoint URL) 와 다르다.
 */
@Slf4j
@Component
public class GoogleIdTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final Set<String> ALLOWED_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param authorizationHeader "Bearer <jwt>" 또는 null
     * @param expectedAudience    Pub/Sub subscription 의 audience (보통 endpoint URL)
     * @return 검증 통과 시 token claims; 실패 시 {@link SqldpassException}({@link ErrorCode#UNAUTHORIZED})
     */
    public JsonNode verify(String authorizationHeader, String expectedAudience) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        String idToken = authorizationHeader.substring(7).trim();
        if (idToken.isEmpty()) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }

        String body;
        try {
            body = restClient.get()
                    .uri(TOKENINFO_URL + idToken)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("tokeninfo 호출 실패", e);
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }

        JsonNode claims;
        try {
            claims = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }

        String iss = claims.has("iss") ? claims.get("iss").asText() : "";
        String aud = claims.has("aud") ? claims.get("aud").asText() : "";
        if (!ALLOWED_ISSUERS.contains(iss)) {
            log.warn("RTDN OIDC iss 불일치 iss={}", iss);
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (expectedAudience == null || expectedAudience.isBlank() || !expectedAudience.equals(aud)) {
            log.warn("RTDN OIDC aud 불일치 expected={} actual={}", expectedAudience, aud);
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return claims;
    }
}
