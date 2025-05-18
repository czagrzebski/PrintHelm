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

    enum TokenType {
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

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getAccessSigningKey())
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

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUsername(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

}
