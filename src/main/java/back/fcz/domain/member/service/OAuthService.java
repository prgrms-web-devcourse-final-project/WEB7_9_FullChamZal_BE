package back.fcz.domain.member.service;

import back.fcz.domain.member.dto.request.OAuthSignupRequest;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.service.PhoneVerificationService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final MemberRepository memberRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final PhoneCrypto phoneCrypto;

    @Transactional
    public MemberSignupResponse oauthSignup(Long memberId, OAuthSignupRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String phoneNumber = request.phoneNumber();
        String phoneHash = phoneCrypto.hash(phoneNumber);

        // 활성 회원 중, 전화번호 중복 여부 체크
        if (memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONENUM);
        }

        // 탈퇴 회원 체크
        if (memberRepository.findByPhoneHashAndDeletedAtIsNotNull(phoneHash).isPresent()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_PHONE_NUMBER);
        }

        boolean verification =
                phoneVerificationService.isPhoneVerified(
                        phoneNumber,
                        PhoneVerificationPurpose.SIGNUP
                );

        if(!verification) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }

        String phoneEncrypted = phoneCrypto.encrypt(phoneNumber);

        member.updateNickname(request.nickname());
        member.updatePhoneNumber(phoneEncrypted, phoneHash);
        memberRepository.save(member);

        return MemberSignupResponse.of(memberId, member.getUserId());
    }
}
