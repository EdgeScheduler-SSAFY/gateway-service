package com.ssafy.gatewayservice.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "role";
    private Key key;

    public JwtTokenProvider(@Value("${token.secret}") String signingKey) {
        byte[] keyBytes = Decoders.BASE64.decode(signingKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    // 토큰 정보를 검증하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.debug("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.debug("JWT claims string is empty.", e);
        }
        return false;
    }

    public String resolveToken(String accessToken) {
        log.debug("accessToken = {}", accessToken);
        if (accessToken != null && accessToken.startsWith(JwtProperties.TOKEN_PREFIX)) {
            return accessToken.substring(7);
        }
        throw new UnsupportedJwtException("지원하지 않는 토큰 형식입니다.");
    }

}
