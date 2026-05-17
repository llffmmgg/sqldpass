package com.sqldpass.service.auth;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.ProtectedHeader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Apple Sign in ID Token 검증.
 *
 * <p>iOS 앱 네이티브 `ASAuthorizationAppleIDProvider` 가 발급한 JWS 를 받아
 * {@code appleid.apple.com/auth/keys} 의 JWKS 로 서명 검증 + iss/aud/exp 확인.</p>
 *
 * <p>JWKS 는 인메모리 캐시. kid miss 시에만 재호출 — Apple 의 키 회전(rare) 대응.</p>
 */
@Slf4j
@Component
public class AppleIdTokenVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String EXPECTED_ISSUER = "https://appleid.apple.com";

    private final String expectedAudience;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** kid → RSAPublicKey 캐시. 키 회전 시 miss → 재로드. */
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    public AppleIdTokenVerifier(
            @Value("${sqldpass.oauth.apple.bundle-id:com.sqldpass.app}") String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    /**
     * 검증 통과 시 Apple {@code sub}(안정적 식별자) 반환.
     */
    public String verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .keyLocator(appleKeyLocator())
                    .requireIssuer(EXPECTED_ISSUER)
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(idToken);
            return jws.getPayload().getSubject();
        } catch (SqldpassException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple ID Token 검증 실패", e);
            throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
        }
    }

    private Locator<PublicKey> appleKeyLocator() {
        return new Locator<>() {
            @Override
            public PublicKey locate(io.jsonwebtoken.Header header) {
                if (!(header instanceof ProtectedHeader protectedHeader)) {
                    throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
                }
                String kid = protectedHeader.getKeyId();
                if (kid == null) {
                    throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED);
                }
                PublicKey cached = keyCache.get(kid);
                if (cached != null) return cached;

                return loadKey(kid).orElseThrow(() ->
                        new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED));
            }
        };
    }

    private Optional<PublicKey> loadKey(String targetKid) {
        try {
            String body = restClient.get()
                    .uri(APPLE_JWKS_URL)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode keys = root.get("keys");
            if (keys == null || !keys.isArray()) return Optional.empty();

            for (JsonNode keyNode : keys) {
                String kid = keyNode.path("kid").asText();
                String kty = keyNode.path("kty").asText();
                if (!targetKid.equals(kid) || !"RSA".equals(kty)) continue;

                BigInteger modulus = new BigInteger(1, base64UrlDecode(keyNode.path("n").asText()));
                BigInteger exponent = new BigInteger(1, base64UrlDecode(keyNode.path("e").asText()));
                PublicKey publicKey = KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));
                keyCache.put(kid, publicKey);
                return Optional.of(publicKey);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Apple JWKS 로드 실패", e);
            return Optional.empty();
        }
    }

    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
