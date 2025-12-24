package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleDashBoardService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final MemberRepository memberRepository;

    // 사용자가 전송한 캡슐 목록 조회
    public Page<CapsuleDashBoardResponse> readSendCapsuleList(Long memberId, Pageable pageable) {
        Page<Capsule> capsules = capsuleRepository.findActiveCapsulesByMemberId(memberId, pageable);

        return capsules.map(capsule -> {
            return new CapsuleDashBoardResponse(capsule);
        });
    }

    // 사용자가 수신한 캡슐 목록 조회
    public Page<CapsuleDashBoardResponse> readReceiveCapsuleList(Long memberId, Pageable pageable) {
        String phoneHash = memberRepository.findById(memberId).orElseThrow(() ->
                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).getPhoneHash();  // 사용자의 해시된 폰 번호

        // 수신자 테이블에서 phoneHash를 가지는 수신자 목록 조회
        Page<CapsuleRecipient> recipients = capsuleRecipientRepository.
                findAllByRecipientPhoneHashWithCapsule(phoneHash, pageable);

        // 수신자가 받은 캡슐 중, 수신자가 삭제하지 않은 캡슐만 조회
        return recipients.map(recipient -> {
            Capsule capsule = recipient.getCapsuleId();
            return new CapsuleDashBoardResponse(capsule);
        });
    }


}
