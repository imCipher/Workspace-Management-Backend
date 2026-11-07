package com.cipher.workspace.Security;

import com.cipher.workspace.Entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessExpirationMs;

    public JwtService(
            @Value("${jwt.secret:1c81cb1987700a52ac1a1e0b51be2a2f82b3995ec6501ee19290e2fae824969d}") String secret,
            @Value("${jwt.issuer:workspacemanagement}") String issuer,
            @Value("${jwt.access-expiration-ms:900000}") long accessExpirationMs) {
        this.signingKey = initSigningKey(secret);
        this.issuer = issuer;
        this.accessExpirationMs = accessExpirationMs;
    }

    private SecretKey initSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDetails user) {
        return generateAccessToken(user, Collections.emptyMap());
    }

    public String generateAccessToken(UserDetails user, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        var builder = Jwts.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .id(UUID.randomUUID().toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)));

        if (user instanceof User appUser) {
            if (appUser.getId() != null) {
                builder.claim("userId", appUser.getId().toString());
            }
            if (appUser.getRole() != null) {
                builder.claim("role", appUser.getRole().name());
            }
            if (appUser.getEmail() != null) {
                builder.claim("email", appUser.getEmail());
            }
            if (appUser.getFullname() != null) {
                builder.claim("fullname", appUser.getFullname());
            }
        }

        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach((key, value) -> {
                if (value != null) {
                    builder.claim(key, value);
                }
            });
        }

        return builder.signWith(signingKey).compact();
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims().getSubject();
        }
    }

    public String extractTokenId(String token) {
        try {
            return extractClaim(token, Claims::getId);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims().getId();
        }
    }

    public UUID extractUserId(String token) {
        String userId;
        try {
            userId = extractClaim(token, claims -> (String) claims.get("userId"));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            userId = (String) e.getClaims().get("userId");
        }
        return userId != null ? UUID.fromString(userId) : null;
    }

    public String extractRole(String token) {
        try {
            return extractClaim(token, claims -> (String) claims.get("role"));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return (String) e.getClaims().get("role");
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            return extractClaim(token, claims -> (List<String>) claims.get("roles"));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return (List<String>) e.getClaims().get("roles");
        }
    }

    public String extractEmail(String token) {
        try {
            return extractClaim(token, claims -> (String) claims.get("email"));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return (String) e.getClaims().get("email");
        }
    }

    public String extractFullname(String token) {
        try {
            return extractClaim(token, claims -> (String) claims.get("fullname"));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return (String) e.getClaims().get("fullname");
        }
    }

    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        }
    }

    public Date extractIssuedAt(String token) {
        try {
            return extractClaim(token, Claims::getIssuedAt);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims().getIssuedAt();
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token, UserDetails user) {
        try {
            if (isTokenExpired(token)) {
                return false;
            }
            final String username = extractUsername(token);
            return username != null && username.equals(user.getUsername());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails user) {
        return isValid(token, user);
    }

    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration != null && expiration.before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }
}
