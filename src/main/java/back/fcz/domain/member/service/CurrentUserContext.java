package back.fcz.domain.member.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 현재 로그인한 사용자 정보 조회
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentUserContext {

    private final MemberRepository memberRepository;

    // 자동으로 현재 사용자 정보를 추출하여 반환
    public InServerMemberResponse getCurrentUser() {
        Long memberId = extractMemberIdFromSecurityContext();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        return InServerMemberResponse.from(member);
    }

    // SecurityContext에서 memberId 추출
    public Long getCurrentMemberId() {
        return extractMemberIdFromSecurityContext();
    }

    private Long extractMemberIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Long)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        return (Long) principal;
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long;
    }
}
