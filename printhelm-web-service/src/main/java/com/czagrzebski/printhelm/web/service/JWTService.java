package com.czagrzebski.printhelm.web.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    public enum TokenType {
        ACCESS,
        REFRESH
    }

    @Value("${ACCESS_JWT_SECRET}")
    private String jwtSecret;

    @Value("${REFRESH_JWT_SECRET}")
    private String refreshJwtSecret;

    public String generateToken(String username, TokenType tokenType) {
        Map<String, Object> claims = new HashMap<>();
        Key key = null;
        long expirationTime = 0L;
        if (tokenType == TokenType.ACCESS) {
            key = getAccessSigningKey();
            expirationTime = 1000 * 60 * 30L; // 30 minutes
        } else if (tokenType == TokenType.REFRESH) {
            key = getRefreshSigningKey();
            expirationTime = 1000 * 60 * 60 * 24L; // 24 hours
        }
        return Jwts.builder().claims().add(claims)
                .subject(username)
                .issuedAt(new java.util.Date(System.currentTimeMillis()))
                .expiration(new java.util.Date(System.currentTimeMillis() + expirationTime))
                .and()
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token, TokenType tokenType) {
        return extractClaim(token, Claims::getSubject, tokenType);
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver, TokenType tokenType) {
        final Claims claims = extractAllClaims(token, tokenType);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, TokenType tokenType) {
        SecretKey key = null;
        if (tokenType == TokenType.ACCESS) {
            key = getAccessSigningKey();
        } else if (tokenType == TokenType.REFRESH) {
            key = getRefreshSigningKey();
        }
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public SecretKey getAccessSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(refreshJwtSecret.getBytes());
    }

    public boolean validateToken(String token, UserDetails userDetails, TokenType tokenType) {
        final String userName = extractUsername(token, tokenType);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token, tokenType));
    }

    private boolean isTokenExpired(String token, TokenType tokenType) {
        return extractExpiration(token, tokenType).before(new Date());
    }

    private Date extractExpiration(String token, TokenType tokenType) {
        return extractClaim(token, Claims::getExpiration, tokenType);
    }

}
