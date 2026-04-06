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

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public GoogleOAuthClient(
            @Value("${sqldpass.oauth.google.client-id}") String clientId,
            @Value("${sqldpass.oauth.google.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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

    public record GoogleUserInfo(String sub, String name) {
    }
}
