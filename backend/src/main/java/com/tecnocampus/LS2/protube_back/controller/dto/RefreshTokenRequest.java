package com.tecnocampus.LS2.protube_back.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank(message = "{validation.refreshToken.required}") String refreshToken) {
}
