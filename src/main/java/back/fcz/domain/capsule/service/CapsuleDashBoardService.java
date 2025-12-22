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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleDashBoardService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final MemberRepository memberRepository;

    // 사용자가 전송한 캡슐 목록 조회
    public List<CapsuleDashBoardResponse> readSendCapsuleList(Long memberId) {
        List<Capsule> capsules = capsuleRepository.findActiveCapsulesByMemberId(memberId);

        List<CapsuleDashBoardResponse> response = capsules.stream()
                .map(capsule -> {
                    // 캡슐의 수신자 조회
                    CapsuleRecipient recipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                            .orElse(null);

                    return new CapsuleDashBoardResponse(capsule, recipient);
                })
                .collect(Collectors.toList());

        return response;
    }

    // 사용자가 수신한 캡슐 목록 조회
    public List<CapsuleDashBoardResponse> readReceiveCapsuleList(Long memberId) {
        String phoneHash = memberRepository.findById(memberId).orElseThrow(() ->
                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).getPhoneHash();  // 사용자의 해시된 폰 번호

        // 수신자 테이블에서 phoneHash를 가지는 수신자 목록 조회
        List<CapsuleRecipient> recipients = capsuleRecipientRepository.findAllByRecipientPhoneHashWithCapsule(phoneHash);

        // 수신자가 받은 캡슐 중, 수신자가 삭제하지 않은 캡슐만 조회
        List<CapsuleDashBoardResponse> response = recipients.stream()
                .filter(recipient -> recipient.getDeletedAt() == null)
                .map(recipient -> {
                    Capsule capsule = recipient.getCapsuleId();

                    return new CapsuleDashBoardResponse(capsule, recipient);
                })
                .collect(Collectors.toList());

        return response;
    }
}
