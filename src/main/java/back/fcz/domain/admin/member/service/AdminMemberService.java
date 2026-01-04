package back.fcz.domain.admin.member.service;

import back.fcz.domain.admin.member.dto.*;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.entity.SanctionType;
import back.fcz.domain.sanction.repository.MemberSanctionHistoryRepository;
import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private static final int NOT_DELETED = 0;
    private static final int PROTECTED = 1; // ✅ 보호:1

    private final MemberRepository memberRepository;
    private final CapsuleRepository capsuleRepository;
    private final ReportRepository reportRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final MemberSanctionHistoryRepository sanctionHistoryRepository;
    private final CurrentUserContext currentUserContext;

    /**
     * 관리자 회원 목록 조회 + 통계(신고당함/보호캡슐/캡슐수)
     * [성능 개선] 3개의 별도 쿼리 → 1개의 JOIN 쿼리로 통합
     */
    public PageResponse<AdminMemberSummaryResponse> searchMembers(AdminMemberSearchRequest cond) {

        Pageable pageable = PageRequest.of(
                cond.getPage(),
                cond.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        LocalDateTime from = cond.getFrom() != null ? cond.getFrom().atStartOfDay() : null;
        LocalDateTime to = cond.getTo() != null ? cond.getTo().plusDays(1).atStartOfDay() : null;

        Page<Member> page = memberRepository.searchAdmin(
                cond.getStatus(),
                cond.getKeyword(),
                from,
                to,
                pageable
        );

        List<Long> memberIds = page.getContent().stream()
                .map(Member::getMemberId)
                .toList();

        // ✅ 성능 개선: 3개의 쿼리를 1개의 JOIN 쿼리로 통합
        Map<Long, AdminMemberStatistics> statsMap = memberRepository
                .getMemberStatisticsBatch(memberIds, PROTECTED)
                .stream()
                .collect(Collectors.toMap(
                        AdminMemberStatistics::getMemberId,
                        stats -> stats
                ));

        Page<AdminMemberSummaryResponse> mapped = page.map(member -> {
            AdminMemberStatistics stats = statsMap.getOrDefault(
                    member.getMemberId(),
                    new AdminMemberStatistics(member.getMemberId(), 0L, 0L, 0L)
            );

            return AdminMemberSummaryResponse.of(
                    member,
                    stats.getReportedCount(),
                    stats.getProtectedCapsuleCount(),
                    stats.getTotalCapsuleCount()
            );
        });

        return new PageResponse<>(mapped);
    }

    /**
     * 회원 상세 조회
     * [성능 개선] 7개 쿼리 → 4개 쿼리로 감소
     * - 쿼리 1: 회원 조회
     * - 쿼리 2: 통계 정보 통합 조회 (캡슐수/보호캡슐수/신고당함수)
     * - 쿼리 3: 최근 캡슐 5개 + 신고수 (Fetch Join)
     * - 쿼리 4: 전화번호 인증 5개
     */
    public AdminMemberDetailResponse getMemberDetail(Long memberId) {

        // 쿼리 1: 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));

        // ✅ 성능 개선: 쿼리 2 - 통계 정보 통합 조회 (3개 쿼리 → 1개 쿼리)
        Object[] statistics = capsuleRepository.getMemberDetailStatistics(
                memberId,
                NOT_DELETED,
                PROTECTED
        );
        
        long totalCapsuleCount = ((Number) statistics[0]).longValue();
        long totalBlockedCapsuleCount = ((Number) statistics[1]).longValue();
        long totalReportCount = ((Number) statistics[2]).longValue();

        // ✅ 성능 개선: 쿼리 3 - 최근 캡슐 5개 + 신고 수 한 번에 조회 (N+1 해결)
        List<Object[]> recentCapsulesWithReportCount = capsuleRepository
                .findTop5WithReportCountByMemberId(memberId, NOT_DELETED);

        List<AdminMemberDetailResponse.RecentCapsuleSummary> recentCapsuleDtos = recentCapsulesWithReportCount.stream()
                .map(row -> {
                    Capsule c = (Capsule) row[0];
                    Long reportCount = ((Number) row[1]).longValue();
                    
                    return AdminMemberDetailResponse.RecentCapsuleSummary.builder()
                            .id(c.getCapsuleId())
                            .title(c.getTitle())
                            .status("ACTIVE") // 미삭제만 가져오므로 ACTIVE 고정
                            .visibility(c.getVisibility())
                            .createdAt(c.getCreatedAt())
                            .openCount(c.getCurrentViewCount())
                            .reportCount(reportCount)
                            .build();
                })
                .toList();

        // 쿼리 4: 전화번호 인증 5개
        List<PhoneVerification> phoneLogs = phoneVerificationRepository
                .findTop5ByPhoneNumberHashOrderByCreatedAtDesc(member.getPhoneHash());

        List<AdminMemberDetailResponse.PhoneVerificationLog> phoneLogDtos = phoneLogs.stream()
                .map(pv -> AdminMemberDetailResponse.PhoneVerificationLog.builder()
                        .id(pv.getId())
                        .purpose(pv.getPurpose().name())
                        .status(pv.getStatus().name())
                        .attemptCount(pv.getAttemptCount())
                        .createdAt(pv.getCreatedAt())
                        .expiredAt(pv.getExpiredAt())
                        .verifiedAt(pv.getVerifiedAt())
                        .build()
                )
                .toList();

        return AdminMemberDetailResponse.builder()
                .id(member.getMemberId())
                .userId(member.getUserId())
                .name(member.getName())
                .nickname(member.getNickname())
                .status(member.getStatus())
                .phoneNumber(member.getPhoneNumber())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .lastNicknameChangedAt(member.getNicknameChangedAt())

                .totalCapsuleCount(totalCapsuleCount)
                .totalReportCount(totalReportCount)
                .totalBookmarkCount(0L)                          // TODO
                .totalBlockedCapsuleCount(totalBlockedCapsuleCount)
                .storyTrackCount(0L)                             // TODO

                .recentCapsules(recentCapsuleDtos)
                .recentNotifications(Collections.emptyList())    // TODO
                .recentPhoneVerifications(phoneLogDtos)
                .build();
    }

    /**
     * 회원 상태 변경 + 제재 이력 저장
     */
    @Transactional
    public AdminMemberStatusUpdateResponse updateMemberStatus(Long memberId, AdminMemberStatusUpdateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));

        Long adminId = currentUserContext.getCurrentMemberId();
        if (Objects.equals(adminId, memberId)) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_CHANGE_SELF_STATUS);
        }

        MemberStatus before = member.getStatus();
        MemberStatus after = request.getStatus();

        if (after == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (before == MemberStatus.EXIT) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_MEMBER_STATUS_CHANGE);
        }

        member.updateStatus(after);

        SanctionType sanctionType = resolveSanctionType(before, after);

        MemberSanctionHistory history = MemberSanctionHistory.create(
                member.getMemberId(),          // ✅ Long
                adminId,
                sanctionType,
                before,                        // ✅ MemberStatus
                after,                         // ✅ MemberStatus
                (request.getReason() == null || request.getReason().isBlank())
                        ? "ADMIN_STATUS_CHANGE"
                        : request.getReason(),
                request.getSanctionUntil()
        );
        sanctionHistoryRepository.save(history);

        return AdminMemberStatusUpdateResponse.of(member, request.getReason(), request.getSanctionUntil());
    }

    private SanctionType resolveSanctionType(MemberStatus before, MemberStatus after) {
        if (before != MemberStatus.STOP && after == MemberStatus.STOP) return SanctionType.STOP;
        if (before == MemberStatus.STOP && after == MemberStatus.ACTIVE) return SanctionType.RESTORE;
        if (after == MemberStatus.EXIT) return SanctionType.EXIT;
        return SanctionType.RESTORE; // 기본값(운영 정책에 맞게 조정 가능)
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        if (rows == null) return map;

        for (Object[] row : rows) {
            Long id = (Long) row[0];
            Long cnt = (Long) row[1];
            map.put(id, cnt);
        }
        return map;
    }
}
