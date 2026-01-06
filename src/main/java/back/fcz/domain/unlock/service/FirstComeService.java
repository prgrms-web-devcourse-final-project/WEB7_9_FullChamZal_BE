package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// 선착순 캡슐 조회수 관리 서비스

@Slf4j
@Service
@RequiredArgsConstructor
public class FirstComeService {

    private final CapsuleRepository capsuleRepository;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private final RedissonClient redissonClient;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final RedisTemplate<String, String> redisTemplate;


    // 선착순 Redisson 락 관련 상수
    private static final String FIRST_COME_LOCK_PREFIX = "capsule:firstcome:lock:";
    private static final long LOCK_WAIT_TIME = 5L;  // 락 획득 대기 시간 (초)
    private static final long LOCK_LEASE_TIME = 2L;  // 락 자동 해제 시간 (초)

    // Redis 조회수 관리 상수
    private static final String VIEW_COUNT_KEY_PREFIX = "capsule:view:";
    private static final Duration VIEW_COUNT_TTL = Duration.ofMinutes(30);

    // 선착순 제한이 있는 캡슐인지 확인
    public boolean hasFirstComeLimit(Capsule capsule) {
        Integer maxViewCount = capsule.getMaxViewCount();
        return maxViewCount != null && maxViewCount > 0;
    }

    // 선착순 캡슐의 남은 인원 수 조회
    public int getRemainingCount(Capsule capsule) {
        if(!hasFirstComeLimit(capsule)) {
            return Integer.MAX_VALUE;
        }

        int remaining = capsule.getMaxViewCount() - capsule.getCurrentViewCount();
        return Math.max(0, remaining);
    }

    @Transactional
    public boolean tryIncrementViewCountAndSaveRecipient(
            Long capsuleId,
            Long memberId,
            CapsuleConditionRequestDTO requestDto
    ) {
        String lockKey = FIRST_COME_LOCK_PREFIX + capsuleId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (대기 시간, 자동 해제 시간)
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("락 획득 실패 - 타임아웃. capsuleId={}, memberId={}", capsuleId, memberId);
                throw new BusinessException(ErrorCode.FIRST_COME_TIMEOUT);
            }

            log.debug("락 획득 성공. capsuleId={}, memberId={}", capsuleId, memberId);

            return processFirstComeWithLock(capsuleId, memberId, requestDto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생. capsuleId={}", capsuleId, e);
            throw new BusinessException(ErrorCode.FIRST_COME_INTERRUPTED);
        } finally {
            // 락 해제 (현재 스레드가 락을 보유한 경우에만)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("락 해제 완료. capsuleId={}", capsuleId);
            }
        }
    }

    // 락을 획득한 상태에서 실제 공개 캡슐 조회 처리
    public boolean processFirstComeWithLock(
            Long capsuleId,
            Long memberId,
            CapsuleConditionRequestDTO requestDto
    ) {
        // 이미 조회한 사용자인지 중복 체크
        boolean alreadyViewed = publicCapsuleRecipientRepository
                .existsByCapsuleId_CapsuleIdAndMemberId(capsuleId, memberId);

        if(alreadyViewed) {
            log.info("이미 조회한 사용자 - 재조회. capsuleId={}, memberId={}", capsuleId, memberId);
            return false;
        }

        // 캡슐 조회
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        // UPDATE 쿼리로 원자적으로 증가 및 마감 체크
        int updatedRows = capsuleRepository.incrementViewCountIfAvailable(capsuleId);

        if (updatedRows == 0) {
            // 업데이트된 행이 없다 = 이미 마감됨
            log.info("선착순 마감. capsuleId={}", capsuleId);
            throw new BusinessException(ErrorCode.FIRST_COME_CLOSED);
        }

        log.info("선착순 조회수 증가 성공. capsuleId={}", capsuleId);

        // 성공 로그 저장
        CapsuleOpenLog openLog = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(memberId)
                .viewerType("MEMBER")
                .status(CapsuleOpenStatus.SUCCESS)
                .anomalyType(AnomalyType.NONE)
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(requestDto.userAgent())
                .ipAddress(requestDto.ipAddress())
                .build();
        capsuleOpenLogRepository.save(openLog);

        //수신자 정보 저장
        PublicCapsuleRecipient recipient = PublicCapsuleRecipient.builder()
                .capsuleId(capsule)
                .memberId(memberId)
                .unlockedAt(requestDto.unlockAt())
                .build();

        publicCapsuleRecipientRepository.save(recipient);

        log.info("선착순 진입 성공. capsuleId={}, memberId={}, currentCount={}/{}",
                capsuleId, memberId, capsule.getCurrentViewCount(), capsule.getMaxViewCount());

        return true;
    }

    // 일반 공개 캡슐
    @Transactional
    public void saveRecipientWithoutFirstCome(
            Long capsuleId,
            Long memberId,
            CapsuleConditionRequestDTO requestDto
    ) {
        boolean alreadyViewed = publicCapsuleRecipientRepository
                .existsByCapsuleId_CapsuleIdAndMemberId(capsuleId, memberId);

        if (alreadyViewed) {
            return;
        }

        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        // 조회수 증가
        incrementViewCountViaRedis(capsuleId);

        // 수신자 정보 저장
        PublicCapsuleRecipient recipient = PublicCapsuleRecipient.builder()
                .capsuleId(capsule)
                .memberId(memberId)
                .unlockedAt(requestDto.unlockAt())
                .build();
        publicCapsuleRecipientRepository.save(recipient);

        log.info("선착순 없음 - 첫 조회 성공. capsuleId={}, memberId={}", capsuleId, memberId);
    }

    // Redis 활용한 조회수 증가
    private void incrementViewCountViaRedis(Long capsuleId) {
        String key = VIEW_COUNT_KEY_PREFIX + capsuleId;

        try {
            Long newCount = redisTemplate.opsForValue().increment(key);

            if (newCount != null && newCount == 1) {
                redisTemplate.expire(key, VIEW_COUNT_TTL);
            }

            log.debug("Redis 조회수 증가 성공 - capsuleId: {}", capsuleId);
        } catch (Exception e) {
            log.error("Redis 장애 발생 - DB 직접 업데이트로 폴백. capsuleId: {}", capsuleId, e);
            capsuleRepository.incrementViewCount(capsuleId);
        }
    }
}
