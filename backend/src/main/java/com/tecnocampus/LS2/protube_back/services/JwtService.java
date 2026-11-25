package com.tecnocampus.LS2.protube_back.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Creates and validates the signed tokens (JWT) that keep a user logged in.
 * <ul>
 *     <li>The <b>access token</b> is sent with every request and lasts a short time (minutes).</li>
 *     <li>The <b>refresh token</b> lasts much longer (days) and is only accepted by {@code POST /api/auth/refresh},
 *     which gives a new pair of tokens. So the session stays open while the user keeps coming back.</li>
 * </ul>
 * Both store the username; a "type" claim tells them apart, so one can never be used as the other.
 */
@Service
public class JwtService {

    static final String TYPE_CLAIM = "type";
    static final String ACCESS = "access";
    static final String REFRESH = "refresh";
    static final long DEFAULT_REFRESH_DAYS = 30;

    private final SecretKey key;
    private final Duration expiration;
    private final Duration refreshExpiration;

    @Autowired
    public JwtService(@Value("${pro_tube.jwt.secret}") String secret,
                      @Value("${pro_tube.jwt.expiration-minutes}") long expirationMinutes,
                      @Value("${pro_tube.jwt.refresh-expiration-days:30}") long refreshExpirationDays) {
        // HMAC-SHA256 needs a key of at least 32 bytes, otherwise this throws on startup
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
        this.refreshExpiration = Duration.ofDays(refreshExpirationDays);
    }

    public JwtService(String secret, long expirationMinutes) {
        this(secret, expirationMinutes, DEFAULT_REFRESH_DAYS);
    }

    public String generateToken(String username) {
        return build(username, ACCESS, expiration);
    }

    public String generateRefreshToken(String username) {
        return build(username, REFRESH, refreshExpiration);
    }

    private String build(String username, String type, Duration validity) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /**
     * @return the username stored in an access token, or empty if the token is invalid, tampered, expired or a
     * refresh token
     */
    public Optional<String> extractUsername(String token) {
        // Tokens issued before the "type" claim existed are access tokens
        return parse(token)
                .filter(claims -> !REFRESH.equals(claims.get(TYPE_CLAIM, String.class)))
                .map(Claims::getSubject);
    }

    /**
     * @return the username stored in a refresh token, or empty if the token is invalid, tampered, expired or an
     * access token
     */
    public Optional<String> extractRefreshUsername(String token) {
        return parse(token)
                .filter(claims -> REFRESH.equals(claims.get(TYPE_CLAIM, String.class)))
                .map(Claims::getSubject);
    }

    private Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
