package back.fcz.domain.member.service;

import back.fcz.domain.member.dto.request.MemberLoginPwRequest;
import back.fcz.domain.member.dto.request.MemberLoginRequest;
import back.fcz.domain.member.dto.request.MemberSignupRequest;
import back.fcz.domain.member.dto.response.LoginTokensResponse;
import back.fcz.domain.member.dto.response.MemberLoginIdResponse;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sms.service.PhoneVerificationService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.security.jwt.JwtProperties;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final PhoneVerificationService phoneVerificationService;

    private static final int VERIFIED_VALID_MINUTES = 10;

    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request) {
        // 활성 회원만 체크
        if (memberRepository.existsByUserIdAndDeletedAtIsNull(request.userId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID);
        }

        // 탈퇴한 회원 체크
        if (memberRepository.findByUserIdAndDeletedAtIsNotNull(request.userId()).isPresent()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER_ID);
        }

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String phoneNumber = request.phoneNumber();
        String phoneHash = phoneCrypto.hash(phoneNumber);

        // 활성 회원만 체크
        if (memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONENUM);
        }

        // 탈퇴 회원 체크
        if (memberRepository.findByPhoneHashAndDeletedAtIsNotNull(phoneHash).isPresent()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_PHONE_NUMBER);
        }

        // TODO: 번호 인증 메서드 추가
//        boolean verification =
//                phoneVerificationService.isPhoneVerified(
//                        phoneNumber,
//                        PhoneVerificationPurpose.SIGNUP
//                );
//
//        if(!verification) {
//            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
//        }

        String phoneEncrypted = phoneCrypto.encrypt(phoneNumber);
        String encryptedPassword = passwordEncoder.encode(request.password());

        Member member = Member.create(
                request.userId(),
                encryptedPassword,
                request.name(),
                request.nickname(),
                phoneEncrypted,
                phoneHash
        );

        Member saved = memberRepository.save(member);

        return MemberSignupResponse.of(saved.getMemberId(), saved.getUserId());
    }

    @Transactional
    public LoginTokensResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByUserId(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_USER_ID));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }

        String accessToken = jwtProvider.generateMemberAccessToken(
                member.getMemberId(),
                member.getRole().name()
        );

        String refreshToken = jwtProvider.generateMemberRefreshToken(
                member.getMemberId(),
                member.getRole().name()
        );

        refreshTokenService.saveMemberRefreshToken(
                member.getMemberId(),
                refreshToken,
                jwtProperties.getRefreshToken().getExpiration() / 1000
        );

        return new LoginTokensResponse(accessToken, refreshToken);
    }

    public MemberLoginIdResponse findUserId(String phoneNumber) {
        // TODO: 번호 인증 확인
//        boolean verification =
//                phoneVerificationService.isPhoneVerified(
//                        phoneNumber,
//                        PhoneVerificationPurpose.FIND_ID
//                );
//
//        if(!verification) {
//            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
//        }

        Member member = memberRepository.findByPhoneHash(phoneCrypto.hash(phoneNumber))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return new MemberLoginIdResponse(member.getUserId());
    }

    @Transactional
    public void findPassword(MemberLoginPwRequest memberLoginPwRequest) {
        // TODO: 번호 인증 확인
//        boolean verification =
//                phoneVerificationService.isPhoneVerified(
//                        memberLoginPwRequest.phoneNum(),
//                        PhoneVerificationPurpose.FIND_PW
//                );
//
//        if(!verification) {
//            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
//        }

        String phoneHash = phoneCrypto.hash(memberLoginPwRequest.phoneNum());

        Member member = memberRepository.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String newPasswordHash = passwordEncoder.encode(memberLoginPwRequest.password());

        member.updatePassword(newPasswordHash);
    }
}
