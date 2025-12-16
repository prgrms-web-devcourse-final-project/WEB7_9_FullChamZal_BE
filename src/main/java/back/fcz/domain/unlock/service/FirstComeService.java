package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 선착순 캡슐 조회수 관리 서비스

@Slf4j
@Service
@RequiredArgsConstructor
public class FirstComeService {

    private final CapsuleRepository capsuleRepository;

    public boolean hasFirstComeLimit(Capsule capsule) {
        return capsule.getMaxViewCount() > 0;
    }

    @Transactional
    public void tryIncrementViewCount(Long capsuleId) {
        Capsule capsule = capsuleRepository.findByIdWithLock(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        if (capsule.getCurrentViewCount() >= capsule.getMaxViewCount()) {
            log.warn("선착순 마감 - capsuleId: {}, current: {}, max: {}",
                    capsuleId, capsule.getCurrentViewCount(), capsule.getMaxViewCount());
            throw new BusinessException(ErrorCode.FIRST_COME_CLOSED);
        }

        capsule.increasedViewCount();

        log.info("선착순 조회수 증가 성공 - capsuleId: {}, newCount: {}/{}",
                capsuleId, capsule.getCurrentViewCount(), capsule.getMaxViewCount());
    }

    public int getRemainingCount(Capsule capsule) {
        if (!hasFirstComeLimit(capsule)) {
            return Integer.MAX_VALUE;
        }

        int remaining = capsule.getMaxViewCount() - capsule.getCurrentViewCount();
        return Math.max(0, remaining);
    }
}
