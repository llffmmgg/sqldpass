package com.sqldpass.controller.auth.dto;

public record OAuthLoginResponse(String token, String nickname, boolean isNew) {
}
