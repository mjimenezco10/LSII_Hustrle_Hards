package com.tecnocampus.LS2.protube_back.controller.dto;

/**
 * A ProTube session: a short-lived access token for the requests, and a long-lived refresh token to get a new one
 * from {@code POST /api/auth/refresh} before it expires.
 */
public record AuthResponse(String token, String refreshToken, UserResponse user) {
}
