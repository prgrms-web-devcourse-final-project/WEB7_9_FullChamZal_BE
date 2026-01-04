package back.fcz.domain.sanction.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.MemberStatusCache;
import back.fcz.domain.sanction.constant.SanctionConstants;
import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.entity.SanctionType;
import back.fcz.domain.sanction.repository.MemberSanctionHistoryRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 회원 제재 처리 서비스 (자동 제재/수동 제재)
@Slf4j
@Service
@RequiredArgsConstructor
public class SanctionService {

    private final MemberRepository memberRepository;
    private final MemberSanctionHistoryRepository sanctionHistoryRepository;
    private final SanctionConstants sanctionConstants;
    private final MemberStatusCache memberStatusCache;

    // 자동 정지 처리 수행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyAutoSuspension(Long memberId, String reason, int days) {
        log.warn("=== [TRACE] applyAutoSuspension 시작 ===");
        log.warn("[TRACE] memberId: {}, reason: {}", memberId, reason);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        log.warn("[TRACE] Member 조회 완료: memberId={}, status={}",
                member.getMemberId(), member.getStatus());

        MemberStatus before = member.getStatus();
        MemberStatus after = MemberStatus.STOP;
        LocalDateTime sanctionUntil = LocalDateTime.now().plusDays(days);

        log.warn("[TRACE] 상태 변경 전: before={}, after={}", before, after);

        // 회원 상태 변경
        member.updateStatus(after);

        log.warn("[TRACE] updateStatus() 호출 완료");
        log.warn("[TRACE] Member 엔티티의 현재 status: {}", member.getStatus());

//        log.warn("자동 정지 처리: memberId={}, 사유={}, 기간={}일, 해제일시={}",
//                memberId, reason, days, sanctionUntil);

        // 시스템 관리자 ID 조회 후 제재 이력 저장
        Long systemAdminId = sanctionConstants.getSystemAdminId();
        String fullReason = sanctionConstants.buildAutoSanctionReason(reason);

        log.warn("[TRACE] 제재 이력 생성 시작");

        MemberSanctionHistory history = MemberSanctionHistory.create(
                memberId,                               // 제재 대상 회원
                systemAdminId,                          // 제재한 관리자 (시스템)
                SanctionType.AUTO_TEMPORARY_SUSPENSION,  // 자동 임시 정지
                before,                                 // 변경 전 상태
                after,                                  // 변경 후 상태 (STOP)
                fullReason,                             // 제재 사유
                sanctionUntil                           // 제재 해제 예정 일시
        );

        log.warn("[TRACE] 제재 이력 저장 시작");
        sanctionHistoryRepository.save(history);
        log.warn("[TRACE] 제재 이력 저장 완료");

        memberStatusCache.invalidateCache(memberId);
        log.warn("[TRACE] 캐시 무효화 완료");

        log.warn("=== [TRACE] applyAutoSuspension 종료 ===");
    }

    // 자동 정지 해제
    @Transactional
    public void restoreSuspension(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        MemberStatus before = member.getStatus();
        MemberStatus after = MemberStatus.ACTIVE;

        // 회원 상태 변경
        member.updateStatus(after);

        // 시스템 관리자 ID 조회
        Long systemAdminId = sanctionConstants.getSystemAdminId();

        // 복구 이력 저장
        MemberSanctionHistory history = MemberSanctionHistory.create(
                memberId,
                systemAdminId,
                SanctionType.RESTORE,  // 복구
                before,
                after,
                "자동 제재 기간 만료로 인한 복구",
                null  // 복구이므로 sanctionUntil은 null
        );

        sanctionHistoryRepository.save(history);
        memberStatusCache.invalidateCache(memberId);

        log.info("자동 정지 해제: memberId={}", memberId);
    }
}
