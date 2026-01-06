package back.fcz.domain.member.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberStatusCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    private static final String STATUS_KEY_PREFIX = "member:status:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public MemberStatus getStatus(Long memberId) {
        String key = STATUS_KEY_PREFIX + memberId;

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("캐시에서 회원 상태 조회: memberId={}, status={}", memberId, cached);
                return MemberStatus.valueOf(cached);
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            redisTemplate.opsForValue().set(key, member.getStatus().name(), CACHE_TTL);
            log.debug("DB에서 회원 상태 조회 및 캐싱: memberId={}, status={}", memberId, member.getStatus());

            return member.getStatus();
        } catch (Exception e) {
            log.error("회원 상태 조회 실패: memberId={}", memberId, e);
            // Redis 장애 시 DB에서 직접 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            return member.getStatus();
        }
    }

    // 회원 상태 변경 시 캐시 무효화
    public void invalidateCache(Long memberId) {
        String key = STATUS_KEY_PREFIX + memberId;
        try {
            redisTemplate.delete(key);
            log.info("회원 상태 캐시 무효화: memberId={}", memberId);
        } catch (Exception e) {
            log.error("캐시 무효화 실패: memberId={}", memberId, e);
        }
    }
}
