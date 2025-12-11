package back.fcz.domain.member.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 특정 사용자 (!= 현재 로그인한 사용자) 정보 조회
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;

    // 회원 ID로 조회
    public InServerMemberResponse getMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return InServerMemberResponse.from(member);
    }

    // 로그인 ID로 조회
    public Optional<InServerMemberResponse> findByUserId(String userId) {
        return memberRepository.findByUserId(userId)
                .map(InServerMemberResponse::from);
    }

    // 전화번호로 조회
    public Optional<InServerMemberResponse> findByPhoneNumber(String plainPhone) {
        String hashedPhone = phoneCrypto.hash(plainPhone);
        return memberRepository.findByPhoneHash(hashedPhone)
                .map(InServerMemberResponse::from);
    }

    // 회원 닉네임 조회
    public String getNicknameById(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getNickname)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
