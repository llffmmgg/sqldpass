package com.sqldpass.controller.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank String code,
        @NotBlank String redirectUri) {
}
