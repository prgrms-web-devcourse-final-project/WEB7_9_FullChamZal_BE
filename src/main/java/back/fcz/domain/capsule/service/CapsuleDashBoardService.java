package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

                    return new CapsuleDashBoardResponse(capsule);
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

                    return new CapsuleDashBoardResponse(capsule);
                })
                .collect(Collectors.toList());

        return response;
    }

    private Pageable createPageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50); // max 50

        return PageRequest.of(safePage, safeSize, sort);
    }

    // 스토리트랙용 캡슐 목록 조회(내가 만든 캡슐, 공개 ,장소 기반)
    public PageResponse<CapsuleDashBoardResponse> myPublicLocationCapsule (Long memberId, int page, int size){
        Pageable pageable = createPageable(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "capsuleId")
        );

        Page<Capsule> capsulePage = capsuleRepository.findMyCapsulesLocationType(memberId, "PUBLIC", "LOCATION", "TIME_AND_LOCATION",pageable);

        Page<CapsuleDashBoardResponse> responsePage = capsulePage.map(CapsuleDashBoardResponse::new);

        return new PageResponse<>(responsePage);
    }
}
