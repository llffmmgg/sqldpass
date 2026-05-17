package com.sqldpass.controller.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 네이티브 안드로이드 앱 — Google Sign-In 이 발급한 ID 토큰으로 로그인.
 * 웹용 OAuth code 흐름과 별개로, 네이티브 SDK 가 만든 ID 토큰을 그대로 백엔드가 검증한다.
 */
public record GoogleIdTokenLoginRequest(@NotBlank String idToken) {
}
