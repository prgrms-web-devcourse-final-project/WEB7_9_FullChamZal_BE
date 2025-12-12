package back.fcz.domain.member.service;

import back.fcz.domain.member.dto.request.MemberLoginRequest;
import back.fcz.domain.member.dto.request.MemberSignupRequest;
import back.fcz.domain.member.dto.response.LoginTokensResponse;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.security.jwt.JwtProperties;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public MemberSignupResponse signup(MemberSignupRequest request) {
        // 활성 회원만 체크
        if (memberRepository.existsByUserIdAndDeletedAtIsNull(request.userId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID);
        }

        // 탈퇴한 회원 체크
        Optional<Member> deletedMember = memberRepository
                .findByUserIdAndDeletedAtIsNotNull(request.userId());
        if (deletedMember.isPresent()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER_ID);
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String normalizedPhone = request.normalizedPhoneNumber();
        if (normalizedPhone == null) {
            throw new BusinessException(ErrorCode.INVALID_PHONENUM);
        }

        String phoneHash = phoneCrypto.hash(normalizedPhone);

        // 활성 회원만 체크
        if (memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONENUM);
        }

        // 탈퇴 회원 체크
        Optional<Member> deletedMemberByPhone = memberRepository
                .findByPhoneHashAndDeletedAtIsNotNull(phoneHash);
        if (deletedMemberByPhone.isPresent()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_PHONE_NUMBER);
        }

        // TODO: 번호 인증 메서드 추가

        String phoneEncrypted = phoneCrypto.encrypt(request.normalizedPhoneNumber());
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

    public LoginTokensResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByUserId(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_USER_ID));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
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
}
