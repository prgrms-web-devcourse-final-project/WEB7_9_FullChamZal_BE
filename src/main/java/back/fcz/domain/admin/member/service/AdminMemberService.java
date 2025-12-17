package back.fcz.domain.admin.member.service;

import back.fcz.domain.admin.member.dto.AdminMemberDetailResponse;
import back.fcz.domain.admin.member.dto.AdminMemberSearchRequest;
import back.fcz.domain.admin.member.dto.AdminMemberStatusUpdateRequest;
import back.fcz.domain.admin.member.dto.AdminMemberStatusUpdateResponse;
import back.fcz.domain.admin.member.dto.AdminMemberSummaryResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final CapsuleRepository capsuleRepository;
    private final ReportRepository reportRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final MemberSanctionHistoryRepository memberSanctionHistoryRepository;
    private final CurrentUserContext currentUserContext;

    /**
     * 관리자용 회원 목록 조회
     */
    public PageResponse<AdminMemberSummaryResponse> searchMembers(AdminMemberSearchRequest cond) {

        LocalDateTime from = toStartOfDay(cond.getFrom());
        LocalDateTime toExclusive = toStartOfNextDay(cond.getTo());

        Pageable pageable = PageRequest.of(
                cond.getPage(),
                cond.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Member> members = memberRepository.searchAdmin(
                cond.getStatus(),
                cond.getKeyword(),
                from,
                toExclusive,
                pageable
        );

        // ✅ 결과가 없으면 바로 리턴 (IN () 방지)
        if (members.isEmpty()) {
            Page<AdminMemberSummaryResponse> empty = members.map(m ->
                    AdminMemberSummaryResponse.of(m, 0L, 0L, 0L)
            );
            return new PageResponse<>(empty);
        }

        List<Long> memberIds = members.getContent().stream()
                .map(Member::getMemberId)
                .toList();

        Map<Long, Long> reportCountMap = toCountMap(reportRepository.countByReporterMemberIds(memberIds));
        Map<Long, Long> capsuleCountMap = toCountMap(capsuleRepository.countActiveByMemberIds(memberIds));
        Map<Long, Long> blockedCapsuleCountMap = toCountMap(
                capsuleRepository.countProtectedActiveByMemberIds(memberIds, 1)
        );

        Page<AdminMemberSummaryResponse> dtoPage = members.map(m ->
                AdminMemberSummaryResponse.of(
                        m,
                        reportCountMap.getOrDefault(m.getMemberId(), 0L),
                        blockedCapsuleCountMap.getOrDefault(m.getMemberId(), 0L),
                        capsuleCountMap.getOrDefault(m.getMemberId(), 0L)
                )
        );

        return new PageResponse<>(dtoPage);
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();

        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            Long cnt = (Long) row[1];
            map.put(id, cnt);
        }
        return map;
    }


    /**
     * 1-2 회원 상세 조회
     * - “최근” 데이터는 너무 많아질 수 있으니 Top5만 내려주는 형태(요약)
     */
    public AdminMemberDetailResponse getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));

        long totalCapsuleCount = capsuleRepository.countByMemberId_MemberIdAndIsDeleted(memberId, 0);
        long totalBlockedCapsuleCount = capsuleRepository.countByMemberId_MemberIdAndIsDeletedAndIsProtected(memberId, 0, 1);

        // 신고 누적: “이 회원이 신고한 횟수” vs “이 회원이 신고당한 횟수”가 애매한데,
        // 현재 스키마상 확실히 계산 가능한 건 "신고한 횟수"이므로 그걸로 넣음.
        // (신고당한 횟수는 Report -> Capsule -> Member 조인 카운트 쿼리 추가하면 가능)
        long totalReportCount = reportRepository.countByReporter_MemberId(memberId);

        // 최근 캡슐 5개 (미삭제만)
        List<Capsule> recentCapsules =
                capsuleRepository.findTop5ByMemberId_MemberIdAndIsDeletedOrderByCreatedAtDesc(memberId, 0);

        List<AdminMemberDetailResponse.RecentCapsuleSummary> recentCapsuleSummaries = recentCapsules.stream()
                .map(c -> AdminMemberDetailResponse.RecentCapsuleSummary.builder()
                        .id(c.getCapsuleId()) // ✅ capsuleId()가 아니라 id()
                        .title(c.getTitle())
                        .status(resolveCapsuleStatus(c))
                        .visibility(c.getVisibility())
                        .createdAt(c.getCreatedAt())
                        .openCount(c.getCurrentViewCount())
                        .reportCount(reportRepository.countByCapsule_CapsuleId(c.getCapsuleId()))
                        .build()
                )
                .collect(Collectors.toList());

        // 최근 전화번호 인증 5개 (member.phoneHash 기준)
        List<PhoneVerification> pvs = (member.getPhoneHash() == null)
                ? List.of()
                : phoneVerificationRepository.findTop5ByPhoneNumberHashOrderByCreatedAtDesc(member.getPhoneHash());

        List<AdminMemberDetailResponse.PhoneVerificationLog> recentPvLogs = pvs.stream()
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
                .collect(Collectors.toList());

        return AdminMemberDetailResponse.builder()
                .id(member.getMemberId())
                .userId(member.getUserId())
                .name(member.getName())
                .nickname(member.getNickname())
                .status(member.getStatus()) // ✅ String이 아니라 MemberStatus
                .phoneNumber(member.getPhoneNumber())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .lastNicknameChangedAt(member.getNicknameChangedAt())

                .totalCapsuleCount(totalCapsuleCount)
                .totalReportCount(totalReportCount)
                .totalBookmarkCount(0L)          // TODO: 즐겨찾기 연동 시 집계
                .totalBlockedCapsuleCount(totalBlockedCapsuleCount)
                .storyTrackCount(0L)             // TODO: StoryTrack 연동 시 집계

                .recentCapsules(recentCapsuleSummaries)
                .recentNotifications(Collections.emptyList()) // TODO: 알림 로그 도메인 연결 시
                .recentPhoneVerifications(recentPvLogs)
                .build();
    }

    /**
     * 1-3 회원 상태 변경
     */
    @Transactional
    public AdminMemberStatusUpdateResponse updateMemberStatus(Long memberId, AdminMemberStatusUpdateRequest request) {
        Long adminId = currentUserContext.getCurrentMemberId();

        if (Objects.equals(adminId, memberId)) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_CHANGE_SELF_STATUS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND));

        MemberStatus before = member.getStatus();
        MemberStatus after = request.getStatus();

        if (before == after) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_MEMBER_STATUS_CHANGE);
        }

        // 정책: EXIT는 복구 금지
        if (before == MemberStatus.EXIT && after != MemberStatus.EXIT) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_MEMBER_STATUS_CHANGE);
        }

        member.updateStatus(after);

        MemberSanctionHistory history = MemberSanctionHistory.create(
                memberId,
                adminId,
                resolveSanctionType(before, after),
                before,
                after,
                (request.getReason() == null || request.getReason().isBlank()) ? "ADMIN_STATUS_CHANGE" : request.getReason(),
                request.getSanctionUntil()
        );
        memberSanctionHistoryRepository.save(history);

        return AdminMemberStatusUpdateResponse.of(member, request.getReason(), request.getSanctionUntil());
    }

    private static LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime toStartOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    private static String resolveCapsuleStatus(Capsule c) {
        Integer isDeleted = c.getIsDeleted();
        Integer isProtected = c.getIsProtected();

        if (isDeleted != null && isDeleted != 0) return "DELETED";
        if (isProtected != null && isProtected == 1) return "PROTECTED";
        return "ACTIVE";
    }

    private static SanctionType resolveSanctionType(MemberStatus before, MemberStatus after) {
        if (after == MemberStatus.STOP) return SanctionType.STOP;
        if (after == MemberStatus.EXIT) return SanctionType.EXIT;
        return SanctionType.RESTORE;
    }
}
