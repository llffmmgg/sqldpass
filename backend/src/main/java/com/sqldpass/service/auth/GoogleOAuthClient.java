package com.sqldpass.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GoogleOAuthClient {

    private static final String ID_TOKEN_VERIFY_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final String clientId;
    private final String androidClientId;
    private final String clientSecret;
    private final RestClient restClient;

    public GoogleOAuthClient(
            @Value("${sqldpass.oauth.google.client-id}") String clientId,
            @Value("${sqldpass.oauth.google.client-secret}") String clientSecret,
            @Value("${sqldpass.oauth.google.android-client-id:}") String androidClientId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.androidClientId = androidClientId;
        this.restClient = RestClient.create();
    }

    public String exchangeCode(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        try {
            JsonNode response = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(JsonNode.class);

            return response.get("access_token").asText();
        } catch (Exception e) {
            log.error("Google OAuth code exchange failed", e);
            throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
    }

    public GoogleUserInfo getUserInfo(String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            String sub = response.get("sub").asText();
            String name = response.has("name") ? response.get("name").asText() : "사용자";
            return new GoogleUserInfo(sub, name);
        } catch (Exception e) {
            log.error("Google OAuth user info fetch failed", e);
            throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
    }

    /**
     * 안드로이드 Capacitor 앱이 네이티브 Google Sign-In 으로 받은 ID 토큰을 검증한다.
     * Google 의 공개 tokeninfo 엔드포인트를 호출 — 서명/만료/issuer 확인은 Google 측에서 끝나고,
     * 우리는 audience(aud) 가 우리 client_id 중 하나와 일치하는지만 추가 검증한다.
     */
    public GoogleUserInfo verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
        try {
            JsonNode response = restClient.get()
                    .uri(ID_TOKEN_VERIFY_URL + idToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("sub")) {
                throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
            }

            String aud = response.has("aud") ? response.get("aud").asText() : "";
            if (!aud.equals(clientId)
                    && !(androidClientId != null && !androidClientId.isBlank() && aud.equals(androidClientId))) {
                log.warn("Google ID token aud mismatch: aud={}", aud);
                throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
            }

            String iss = response.has("iss") ? response.get("iss").asText() : "";
            if (!iss.equals("accounts.google.com") && !iss.equals("https://accounts.google.com")) {
                log.warn("Google ID token iss invalid: iss={}", iss);
                throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
            }

            String sub = response.get("sub").asText();
            String name = response.has("name") ? response.get("name").asText() : "사용자";
            return new GoogleUserInfo(sub, name);
        } catch (SqldpassException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google ID token verification failed", e);
            throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
    }

    public record GoogleUserInfo(String sub, String name) {
    }
}
