package back.fcz.global.security.jwt;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_ROLE = "role";
    private static final String SUBJECT_PREFIX_MEMBER = "member:";
    private static final String SUBJECT_PREFIX_GUEST = "guest:";
    private static final String SUBJECT_PREFIX_ADMIN = "admin:";

    public JwtProvider(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.accessTokenExpiration = jwtProperties.getAccessToken().getExpiration();
        this.refreshTokenExpiration = jwtProperties.getRefreshToken().getExpiration();
    }


    // ---------- Member 도메인용 ----------

    // 회원용 AccessToken 생성
    public String generateMemberAccessToken(Long memberId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(SUBJECT_PREFIX_MEMBER + memberId) // "member:123" 형식으로 Redis에 저장
                .claim(CLAIM_USER_TYPE, UserType.MEMBER.name())
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // 회원용 RefreshToken 생성
    public String generateMemberRefreshToken(Long memberId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(SUBJECT_PREFIX_MEMBER + memberId)
                .claim(CLAIM_USER_TYPE, UserType.MEMBER.name())
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // ---------- Admin 도메인용 ----------

    // 관리자용 AccessToken 생성
    public String generateAdminAccessToken(Long memberId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(SUBJECT_PREFIX_ADMIN + memberId) // "admin:123" 형식으로 Redis에 저장
                .claim(CLAIM_USER_TYPE, UserType.ADMIN.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // 관리자용 RefreshToken 생성
    public String generateAdminRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(SUBJECT_PREFIX_ADMIN + memberId)
                .claim(CLAIM_USER_TYPE, UserType.ADMIN.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // ---------- 공통 - 토큰 정보 추출 ----------

    // 토큰에서 Claims 추출
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰 만료: 만료 시간 = {}", e.getClaims().getExpiration());
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 형식: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_UNSUPPORTED);
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_MALFORMED);
        } catch (SignatureException e) {
            log.error("JWT 서명 검증 실패 (보안 이슈 가능성): token prefix = {}",
                    token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
            throw new BusinessException(ErrorCode.TOKEN_SIGNATURE_INVALID);
        } catch (IllegalArgumentException e) {
            log.warn("빈 JWT 토큰 또는 null: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_EMPTY);
        } catch (Exception e) {
            log.error("JWT 토큰 파싱 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    // 토큰에서 UserType 추출
    public UserType extractUserType(String token) {
        Claims claims = extractClaims(token);
        String userType = claims.get(CLAIM_USER_TYPE, String.class);

        try {
            return UserType.valueOf(userType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    // 토큰에서 사용자 ID 추출
    public Long extractMemberId(String token) {
        Claims claims = extractClaims(token);
        String subject = claims.getSubject();

        if (subject == null) {
            throw new BusinessException(ErrorCode.TOKEN_SUBJECT_INVALID);
        }

        String[] parts = subject.split(":");
        if(parts.length != 2) {
            throw new BusinessException(ErrorCode.TOKEN_SUBJECT_INVALID);
        }

        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.TOKEN_SUBJECT_INVALID);
        }
    }

    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    // ---------- 공통 - 토큰 검증 ----------

    // 토큰 만료 확인
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (BusinessException e) {
            return true;
        }
    }

    /**
     * 토큰의 사용자 타입이 예상하는 타입과 일치하는지 검증
     * 회원/어드민 전용 도메인에서 로그인한 사용자가 회원인지 검증하는 용도로 사용하는 헬퍼 메서드
     * */
    public boolean isUserTypeMatching(String token, UserType expectedType) {
        try {
            UserType actualType = extractUserType(token);
            return actualType == expectedType;
        } catch (BusinessException e) {
            return false;
        }
    }

    // 토큰에서 만료 시간(초) 추출
    public long getExpirationSeconds(String token) {
        Claims claims = extractClaims(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();

        long diffMillis = expiration.getTime() - now.getTime();
        return Math.max(0, diffMillis/1000);
    }
}
