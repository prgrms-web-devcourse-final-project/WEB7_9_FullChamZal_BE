package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleSaveButtonRequest;
import back.fcz.domain.capsule.DTO.response.CapsuleSaveButtonResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CapsuleSaveButtonService {
    private final CurrentUserContext currentUserContext;
    private final MemberRepository memberRepository;
    private final CapsuleRepository  capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;

    public CapsuleSaveButtonResponse saveRecipient(CapsuleSaveButtonRequest request) {
        //로그인 상태라면 개인 캡슐 수신자 정보 생성(현재 로그인 중인 회원의 데이터 기록)
        if (!isUserLoggedIn()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long memberId = currentUserContext.getCurrentMemberId();

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Capsule capsule = capsuleRepository.findById(request.capsuleId()).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        boolean alreadySaved = capsuleRecipientRepository
                .existsByCapsuleId_CapsuleIdAndRecipientPhoneHash(
                        capsule.getCapsuleId(),
                        member.getPhoneHash()
                );

        if (alreadySaved) {
            log.warn("이미 저장된 캡슐 - memberId: {}, capsuleId: {}", memberId, capsule.getCapsuleId());
            throw new BusinessException(ErrorCode.CAPSULE_ALREADY_SAVED);
        }

        if ("PUBLIC".equals(capsule.getVisibility())) {
            log.warn("공개 캡슐 저장 시도 - capsuleId: {}", capsule.getCapsuleId());
            throw new BusinessException(ErrorCode.PUBLIC_CAPSULE_CANNOT_BE_SAVED);
        }

        int isSenderSelf = 0;
        if(capsule.getVisibility().equals("SELF") || capsule.getVisibility() == "SELF") {
            isSenderSelf = 1;
        }

        LocalDateTime unlockedAt = findFirstOpenedAt(capsule.getCapsuleId(), memberId);

        CapsuleRecipient capsuleRecipient = CapsuleRecipient.builder()
                .capsuleId(capsule)
                .recipientName(member.getNickname())
                .recipientPhone(member.getPhoneNumber())
                .recipientPhoneHash(member.getPhoneHash())
                .isSenderSelf(isSenderSelf)
                .unlockedAt(unlockedAt)
                .build();

        capsuleRecipientRepository.save(capsuleRecipient);
        capsule.setProtected(1);
        capsuleRepository.save(capsule);

        return new CapsuleSaveButtonResponse("캡슐이 저장 되었습니다.");
    }

    private LocalDateTime findFirstOpenedAt(Long capsuleId, Long memberId) {
        return capsuleOpenLogRepository
                .findFirstByCapsuleId_CapsuleIdAndMemberId_MemberIdOrderByOpenedAtAsc(capsuleId, memberId)
                .map(CapsuleOpenLog::getOpenedAt)
                .orElseGet(() -> {
                    log.warn("CapsuleOpenLog 없음 - 현재 시각으로 fallback, capsuleId: {}", capsuleId);
                    throw new BusinessException(ErrorCode.CAPSULE_OPEN_LOG_NOT_FOUND);
                });
    }

    // 사용자 로그인 여부
    private boolean isUserLoggedIn() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            Object principal = authentication.getPrincipal();
            return principal instanceof Long;
        } catch (BusinessException e) {
            return false;
        }
    }
}
