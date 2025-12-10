package back.fcz.global.security.jwt.service;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT Refresh Token 관리 서비스
 *
 * Refresh Token을 Redis에 저장하여 관리
 * 로그아웃, 비밀번호 변경 등의 경우 강제로 무효화 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;

    private static final String REFRESH_TOKEN_KEY_PREFIX_MEMBER = "refresh:member:";
    private static final String REFRESH_TOKEN_KEY_PREFIX_ADMIN = "refresh:admin:";

    // 회원 Refresh Token 저장
    public void saveMemberRefreshToken(Long memberId, String refreshToken, long expirationSeconds) {
        saveRefreshToken(UserType.MEMBER, memberId, refreshToken, expirationSeconds);
    }

    // 관리자 Refresh Token 저장
    public void saveAdminRefreshToken(Long memberId, String refreshToken, long expirationSeconds) {
        saveRefreshToken(UserType.ADMIN, memberId, refreshToken, expirationSeconds);
    }

    // 회원 Refresh Token 삭제
    public void deleteMemberRefreshToken(Long memberId) {
        deleteRefreshToken(UserType.MEMBER, memberId);
    }

    // 관리자 Refresh Token 삭제
    public void deleteAdminRefreshToken(Long memberId) {
        deleteRefreshToken(UserType.ADMIN, memberId);
    }

    // Refresh Token 저장
    private void saveRefreshToken(UserType userType, Long memberId, String refreshToken, long expirationSeconds) {
        try {
            String key = getKeyByUserType(userType, memberId);

            redisTemplate.opsForValue().set(
                    key,
                    refreshToken,
                    expirationSeconds,
                    TimeUnit.SECONDS
            );

            log.info("{} Refresh Token 저장 완료. memberId: {}, TTL: {}초",
                    getUserTypeDisplayName(userType), memberId, expirationSeconds);
        } catch (Exception e) {
            log.error("{} Refresh Token 저장 중 오류 발생: {}",
                    getUserTypeDisplayName(userType), e.getMessage(), e);
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }
    }

    // Refresh Token 삭제
    private void deleteRefreshToken(UserType userType, Long memberId) {
        try {
            String key = getKeyByUserType(userType, memberId);
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("{} Refresh Token 삭제 완료. memberId: {}",
                        getUserTypeDisplayName(userType), memberId);
            } else {
                log.warn("삭제할 {} Refresh Token이 없습니다. memberId: {}",
                        getUserTypeDisplayName(userType), memberId);
            }

        } catch (Exception e) {
            log.error("{} Refresh Token 삭제 중 오류 발생: {}",
                    getUserTypeDisplayName(userType), e.getMessage(), e);
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }
    }

    // Refresh Token 검증
    public void validateRefreshToken(String refreshToken) {
        // 1. JWT 형식 검증
        UserType userType = jwtProvider.extractUserType(refreshToken);
        Long userId = jwtProvider.extractMemberId(refreshToken);

        // 2. Redis 저장 토큰과 비교
        String key = getKeyByUserType(userType, userId);
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null) {
            log.warn("Redis에 Refresh Token이 없습니다. userType: {}, userId: {}", userType, userId);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        if (!storedToken.equals(refreshToken)) {
            log.warn("Refresh Token이 일치하지 않습니다. userType: {}, userId: {}", userType, userId);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        log.debug("Refresh Token 검증 성공. userType: {}, userId: {}", userType, userId);
    }

    // Refresh Token으로 새로운 Access Token 발급
    public String refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 검증
        validateRefreshToken(refreshToken);

        // 2. 사용자 정보 추출
        UserType userType = jwtProvider.extractUserType(refreshToken);
        Long userId = jwtProvider.extractMemberId(refreshToken);

        // 3. 사용자 타입에 따라 새 Access Token 발급
        if (userType == UserType.MEMBER) {
            String role = jwtProvider.extractRole(refreshToken);
            return jwtProvider.generateMemberAccessToken(userId, role);
        } else {
            return jwtProvider.generateAdminAccessToken(userId);
        }
    }

    // 헬퍼 메서드 - UserType에 따른 Redis 키 생성
    private String getKeyByUserType(UserType userType, Long userId) {
        if (userType == UserType.MEMBER) {
            return REFRESH_TOKEN_KEY_PREFIX_MEMBER + userId;
        } else {
            return REFRESH_TOKEN_KEY_PREFIX_ADMIN + userId;
        }
    }

    // 헬퍼 메서드 - 로그 출력용
    private String getUserTypeDisplayName(UserType userType) {
        return userType == UserType.MEMBER ? "회원" : "관리자";
    }
}
