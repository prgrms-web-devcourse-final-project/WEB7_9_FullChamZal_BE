package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleReceiveDashBoardResponseDto;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CapsuleDashBoardService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final MemberRepository memberRepository;

    public List<Capsule> readSendCapsuleList(Long memberId) {
        return capsuleRepository.findActiveCapsulesByMemberId(memberId);
    }

    public List<CapsuleReceiveDashBoardResponseDto> readReceiveCapsuleList(Long memberId) {
        String phoneHash = memberRepository.findById(memberId).orElseThrow(() ->
                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).getPhoneHash();  // 사용자의 해시된 폰 번호

        // 수신자 테이블에서 phoneHash를 가지는 수신자 목록 조회
        List<CapsuleRecipient> recipients = capsuleRecipientRepository.findAllByRecipientPhoneHashWithCapsule(phoneHash);

        // 수신자가 받은 캡슐 조회
        List<CapsuleReceiveDashBoardResponseDto> response = recipients.stream()
                .map(recipient -> {
                    Capsule capsule = recipient.getCapsuleId();

                    return new CapsuleReceiveDashBoardResponseDto(capsule, recipient);
                })
                .collect(Collectors.toList());

        return response;
    }
}
