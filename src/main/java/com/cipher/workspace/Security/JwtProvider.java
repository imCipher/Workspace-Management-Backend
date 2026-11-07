package com.cipher.workspace.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Convenience component extending JwtService for code referencing JwtProvider.
 */
@Component
public class JwtProvider extends JwtService {

    public JwtProvider(
            @Value("${jwt.secret:1c81cb1987700a52ac1a1e0b51be2a2f82b3995ec6501ee19290e2fae824969d}") String secret,
            @Value("${jwt.issuer:workspacemanagement}") String issuer,
            @Value("${jwt.access-expiration-ms:900000}") long accessExpirationMs) {
        super(secret, issuer, accessExpirationMs);
    }
}
