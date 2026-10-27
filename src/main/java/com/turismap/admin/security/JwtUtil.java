package com.turismap.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "La propiedad jwt.secret no está configurada"
            );
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "La clave JWT debe tener al menos 32 bytes para HS256"
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private long getExpirationMillis() {
        if (expiration == null || expiration <= 0) {
            throw new IllegalStateException(
                    "La propiedad jwt.expiration debe ser mayor que cero"
            );
        }

        return expiration;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        Date expirationDate = extractExpiration(token);

        return expirationDate == null
                || expirationDate.before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        return createToken(
                claims,
                userDetails.getUsername()
        );
    }

    public String generateToken(String username, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);

        return createToken(claims, username);
    }

    private String createToken(
            Map<String, Object> claims,
            String subject
    ) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "El usuario del token no puede estar vacío"
            );
        }

        long now = System.currentTimeMillis();
        long expirationMillis = getExpirationMillis();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMillis))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(
            String token,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            return false;
        }

        return validateToken(token, userDetails.getUsername());
    }

    public boolean validateToken(
            String token,
            String username
    ) {
        if (token == null || token.isBlank()
                || username == null || username.isBlank()) {
            return false;
        }

        try {
            Claims claims = extractAllClaims(token);
            String extractedUsername = claims.getSubject();
            Date expirationDate = claims.getExpiration();

            return extractedUsername != null
                    && extractedUsername.equals(username)
                    && expirationDate != null
                    && expirationDate.after(new Date());

        } catch (io.jsonwebtoken.JwtException
                 | IllegalArgumentException ex) {
            return false;
        }
    }
}
