package com.sqldpass.service.pdf;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Playwright 가 인쇄 페이지를 렌더할 때 백엔드 정답·해설 데이터(/api/internal/print/mock-exams/{id})
 * 를 받아갈 수 있도록 발급되는 단기 (2분) 토큰.
 *
 * - 어드민이 PDF 생성 요청을 보낼 때 발급
 * - subject 에 "pdf-print:{mockExamId}" 를 박아둠 → 다른 모의고사 데이터에 재사용 불가
 * - HMAC SHA-256 서명, 어드민 JWT 와 동일한 secret 사용 (별도 키 운영 부담 회피)
 */
@Service
public class PrintTokenService {

    private static final String SUBJECT_PREFIX = "pdf-print:";
    private static final long TTL_MILLIS = 2 * 60 * 1000L;  // 2분

    private final SecretKey key;

    public PrintTokenService(@Value("${sqldpass.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String issue(Long mockExamId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(SUBJECT_PREFIX + mockExamId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL_MILLIS))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 검증 + 토큰의 subject 가 요청 경로의 mockExamId 와 일치하는지 확인.
     * 일치하지 않거나 만료/위조 시 false.
     */
    public boolean validate(String token, Long mockExamId) {
        if (token == null || mockExamId == null) return false;
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            return subject != null && subject.equals(SUBJECT_PREFIX + mockExamId);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
