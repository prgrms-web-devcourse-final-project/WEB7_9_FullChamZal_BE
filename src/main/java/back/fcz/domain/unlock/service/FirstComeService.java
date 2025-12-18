package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 선착순 캡슐 조회수 관리 서비스

@Slf4j
@Service
@RequiredArgsConstructor
public class FirstComeService {

    private final CapsuleRepository capsuleRepository;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;

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


    // 선착순 검증, 조회수 증가, PublicCapsuleRecipient 저장 O (비관적 락 사용)
    @Transactional
    public boolean tryIncrementViewCountAndSaveRecipient(
            Long capsuleId,
            Long memberId,
            LocalDateTime unlockedAt
    ) {
        // 1. 이미 조회한 사용자인지 확인
        boolean alreadyViewed = publicCapsuleRecipientRepository
                .existsByCapsuleId_CapsuleIdAndMemberId(capsuleId, memberId);

        if (alreadyViewed) {
            log.info("이미 조회한 사용자. capsuleId={}, memberId={}", capsuleId, memberId);
            return false; // 재조회
        }

        // 2. 원자적으로 조회수 증가 시도
        // UPDATE 쿼리가 조건을 만족하는 경우에만 실행되고, 업데이트된 행의 개수를 반환
        int updatedRows = capsuleRepository.incrementViewCountIfAvailable(capsuleId);

        if (updatedRows == 0) {
            // 업데이트 실패 = 이미 선착순 마감됨
            log.info("선착순 마감. capsuleId={}", capsuleId);
            throw new BusinessException(ErrorCode.FIRST_COME_CLOSED);
        }

        // 3. 수신자 정보 저장
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        PublicCapsuleRecipient recipient = PublicCapsuleRecipient.builder()
                .capsuleId(capsule)
                .memberId(memberId)
                .unlockedAt(unlockedAt)
                .build();
        publicCapsuleRecipientRepository.save(recipient);

        log.info("선착순 진입 성공 및 수신자 저장 완료. capsuleId={}, memberId={}, currentCount={}/{}",
                capsuleId, memberId, capsule.getCurrentViewCount(), capsule.getMaxViewCount());

        return true; // 새로운 조회
    }

    // 일반 공개 캡슐 조회수 증가
    @Transactional
    public void incrementViewCount(Long capsuleId) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        capsule.increasedViewCount();

        log.debug("일반 조회수 증가. capsuleId={}, newCount={}",
                capsuleId, capsule.getCurrentViewCount());
    }
}
