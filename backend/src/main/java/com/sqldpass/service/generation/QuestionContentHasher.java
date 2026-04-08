package com.sqldpass.service.generation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 모의고사 회차 간/회차 내 본문 중복 검증용 해시 유틸.
 *
 * normalize 단계에서 공백/대소문자/숫자/마크다운 잡음을 제거해
 * "의미상 동일" 여부에 가깝게 만든 뒤 SHA-256으로 고정 길이 해시.
 *
 * 정처기/SQLD/컴활 모두 동일한 정의 사용.
 */
public final class QuestionContentHasher {

    private QuestionContentHasher() {}

    /**
     * 본문 정규화:
     * - 마크다운 코드 펜스/백틱/별표/언더스코어 제거
     * - 모든 공백류 1칸 압축
     * - 숫자 → '#' 마스킹 (출제 데이터값 차이 무시)
     * - 소문자
     */
    public static String normalize(String content) {
        if (content == null) return "";
        String s = content;
        s = s.replaceAll("```[a-zA-Z]*", " ");
        s = s.replace("`", " ");
        s = s.replaceAll("[*_~]", " ");
        s = s.replaceAll("\\s+", " ");
        s = s.replaceAll("\\d+", "#");
        return s.trim().toLowerCase();
    }

    /** normalize 후 SHA-256 hex (64자) */
    public static String hashOf(String content) {
        String norm = normalize(content);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(norm.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원 환경", e);
        }
    }
}
