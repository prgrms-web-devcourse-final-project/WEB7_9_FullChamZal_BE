package back.fcz.domain.sanction.scheduler;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.entity.SanctionType;
import back.fcz.domain.sanction.repository.MemberSanctionHistoryRepository;
import back.fcz.domain.sanction.service.SanctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 자동 제재 해제 (매시간 정각)
@Slf4j
@Component
@RequiredArgsConstructor
public class SanctionScheduler {

    private final MemberSanctionHistoryRepository sanctionHistoryRepository;
    private final MemberRepository memberRepository;
    private final SanctionService sanctionService;

    /**
     * sanctionUntil이 현재 시간 이전인 자동 정지 건을 조회
     * 해당 회원이 여전히 STOP 상태이고, 이후 복구 이력이 없으면 자동 해제
     */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void releaseExpiredSuspensions() {
        log.info("=== 자동 제재 해제 스케줄러 시작 ===");

        LocalDateTime now = LocalDateTime.now();

        // 만료된 자동 정지 이력 조회
        List<MemberSanctionHistory> expiredSanctions = sanctionHistoryRepository
                .findExpiredAutoSuspensions(SanctionType.AUTO_TEMPORARY_SUSPENSION, now);
        if (expiredSanctions.isEmpty()) {
            log.info("자동 해제 대상 없음");
            return;
        }

        log.info("자동 해제 대상 발견: {}건", expiredSanctions.size());

        int successCount = 0;
        int skipCount = 0;

        for (MemberSanctionHistory history : expiredSanctions) {
            Long memberId = history.getMemberId();

            try {
                // 회원 조회
                Member member = memberRepository.findById(memberId).orElse(null);

                if (member == null) {
                    log.warn("회원 없음 - memberId: {}, 해제 건너뜀", memberId);
                    skipCount++;
                    continue;
                }

                // 현재 상태가 STOP인지 확인 (중간에 다른 제재가 걸렸을 수 있음)
                if (member.getStatus() != MemberStatus.STOP) {
                    log.info("회원 상태가 STOP이 아님 - memberId: {}, status: {}, 해제 건너뜀",
                            memberId, member.getStatus());
                    skipCount++;
                    continue;
                }

                // 자동 해제 수행
                sanctionService.restoreSuspension(memberId);

                log.info("자동 제재 해제 완료 - memberId: {}, sanctionId: {}, sanctionUntil: {}",
                        memberId, history.getId(), history.getSanctionUntil());

                successCount++;

            } catch (Exception e) {
                log.error("자동 제재 해제 실패 - memberId: {}, sanctionId: {}",
                        memberId, history.getId(), e);
                skipCount++;
            }
        }

        log.info("=== 자동 제재 해제 스케줄러 종료 - 성공: {}건, 건너뜀: {}건 ===",
                successCount, skipCount);
    }
}
