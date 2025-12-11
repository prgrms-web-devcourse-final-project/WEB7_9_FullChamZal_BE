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
import back.fcz.global.security.jwt.JwtProvider;
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

    public MemberSignupResponse signup(MemberSignupRequest request) {
        if (memberRepository.existsByUserId(request.userId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID);
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String normalizedPhone = request.normalizedPhoneNumber();
        String phoneHash = phoneCrypto.hash(normalizedPhone);

        if (memberRepository.existsByPhoneHash(phoneHash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONENUM);
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
        Member member = memberRepository.findByUserId(request.userId());

        if (member == null) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID);
        }

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

        return new LoginTokensResponse(accessToken, refreshToken);
    }
}
