package com.manacommunity.sports.security;

import com.manacommunity.sports.user.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies signed JWT access tokens (HMAC-SHA).
 *
 * <p>The signing secret comes from configuration ({@code app.security.jwt.secret},
 * env-driven in prod) and must be at least 32 bytes for HS256. Tokens carry:
 * <ul>
 *   <li>{@code iss} — issuer ({@code app.security.jwt.issuer})</li>
 *   <li>{@code sub} — the user's email</li>
 *   <li>{@code uid} — the user's database id</li>
 *   <li>{@code role} — the user's role</li>
 *   <li>{@code typ} — token type: {@code access} or {@code refresh}</li>
 *   <li>{@code iat}/{@code exp} — issued-at and expiry</li>
 * </ul>
 * Verification checks the signature, the issuer and the expiry on every request.</p>
 *
 * <p>Two token kinds are issued (stateless — no server-side store):
 * a short-lived <b>access</b> token used as the {@code Bearer} credential on every API
 * call, and a long-lived <b>refresh</b> token the client exchanges (via {@code /api/auth/refresh})
 * for a fresh pair when the access token expires. Because there is no store, neither can be
 * revoked before its {@code exp}; logout/ban take effect at most one access-token lifetime later.</p>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** Token-type claim name + values. */
    public static final String TYPE_CLAIM = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final String issuer;

    private static final String INSECURE_DEFAULT_PREFIX = "dev-only-insecure-secret";

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:900000}") long expirationMs,
            @Value("${app.security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            @Value("${app.security.jwt.issuer:mana-community}") String issuer,
            @Value("${app.security.mock-auth-enabled:false}") boolean mockAuthEnabled) {

        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be at least 32 characters (256 bits) for HS256.");
        }
        if (!mockAuthEnabled && secret.startsWith(INSECURE_DEFAULT_PREFIX)) {
            throw new IllegalStateException(
                    "The default dev JWT secret must not be used outside of local development. "
                    + "Set the JWT_SECRET environment variable to a unique, random value.");
        }
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
    }

    /** Mints a signed, short-lived <b>access</b> token (the API Bearer credential). */
    public String generateToken(AppUser user) {
        return build(user, TYPE_ACCESS, expirationMs);
    }

    /** Mints a signed, long-lived <b>refresh</b> token (exchanged at {@code /api/auth/refresh}). */
    public String generateRefreshToken(AppUser user) {
        return build(user, TYPE_REFRESH, refreshExpirationMs);
    }

    private String build(AppUser user, String type, long ttlMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole())
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMs)))
                .signWith(key) // HS256 — algorithm inferred from key size
                .compact();
    }

    /** True when the token's signature, issuer and expiry all check out. */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /** True when the token is valid AND an <b>access</b> token (rejects refresh tokens as Bearer). */
    public boolean isAccessToken(String token) {
        try {
            return !TYPE_REFRESH.equals(getTokenType(token));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** True when the token is valid AND a <b>refresh</b> token. */
    public boolean isRefreshToken(String token) {
        try {
            return TYPE_REFRESH.equals(getTokenType(token));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** The {@code typ} claim ({@code access}/{@code refresh}), or null when absent (legacy tokens). */
    public String getTokenType(String token) {
        Object typ = parse(token).getPayload().get(TYPE_CLAIM);
        return typ == null ? null : typ.toString();
    }

    /** The authenticated user's id, or null if the {@code uid} claim is absent/invalid. */
    public Long getUserId(String token) {
        Object uid = parse(token).getPayload().get("uid");
        return uid instanceof Number n ? n.longValue() : null;
    }

    public String getEmail(String token) {
        return parse(token).getPayload().getSubject();
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }
}
