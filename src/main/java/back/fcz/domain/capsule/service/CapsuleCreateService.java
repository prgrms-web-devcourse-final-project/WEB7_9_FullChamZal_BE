package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleUpdateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleCreateResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDeleteResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleUpdateResponseDTO;
import back.fcz.domain.capsule.DTO.response.SecretCapsuleCreateResponseDTO;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.*;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.openai.moderation.dto.CapsuleModerationBlockedPayload;
import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.service.CapsuleModerationService;
import back.fcz.domain.sms.service.SmsNotificaationService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static io.micrometer.common.util.StringUtils.isBlank;

@Service
@Transactional
@RequiredArgsConstructor
public class CapsuleCreateService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final CapsuleRecipientRepository recipientRepository;
    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final PublicCapsuleRecipientRepository publicRecipientRepository;
    private final SmsNotificaationService smsNotificaationService;

    // ✅ moderation
    private final CapsuleModerationService capsuleModerationService;

    // 캡슐 첨부 파일
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;

    // url 도메인
    @Value("${cors.capsule-domain}")
    private String domain;

    // UUID 생성
    public String setUUID() {
        return UUID.randomUUID().toString();
    }

    // 캡슐 비밀번호 생성 - 4자리 숫자 번호
    private String generatePassword() {
        Random random = new Random();
        int number = random.nextInt(9000) + 1000;
        return String.valueOf(number);
    }

    public void isCapsuleProfileIncomplete(Capsule capsule){
        // 닉네임 존재 확인
        // "" 공백 닉네임도 허용
        if(capsule.getNickname() == null){
            throw new BusinessException(ErrorCode.NICKNAME_REQUIRED);
        }

        // phone 존재 확인
        // 휴대전화가 null이거나 공백일 때
        if(capsule.getMemberId().getPhoneNumber() == null || isBlank(capsule.getMemberId().getPhoneNumber())
        || capsule.getMemberId().getPhoneHash() == null || isBlank(capsule.getMemberId().getPhoneHash())
        ){
            throw new BusinessException(ErrorCode.PHONENUMBER_REQUIRED);
        }
    }

    /**
     * 공개 캡슐 생성
     * - moderation flagged이면 생성 자체를 막고(CPS011) payload로 위반 필드/카테고리 내려줌 (CapsuleModerationService에서 처리)
     * - ✅ PASS/SKIPPED는 로그 저장 안 함, 실패만 저장
     */
    @Transactional
    public CapsuleCreateResponseDTO publicCapsuleCreate(CapsuleCreateRequestDTO capsuleCreate) {
        Capsule capsule = capsuleCreate.toEntity();

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // ✅ 저장 직전 moderation 검사 (flagged면 서비스에서 예외 던짐 / PASS는 아무것도 저장 안 함)
        capsuleModerationService.validateCapsuleText(
                member.getMemberId(),
                ModerationActionType.CAPSULE_CREATE,
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getReceiverNickname(),
                capsule.getLocationName(),
                capsule.getAddress()
        );

        capsule.setMemberId(member);

        isCapsuleProfileIncomplete(capsule);

        capsule.setUuid(setUUID());
        Capsule saved = capsuleRepository.save(capsule);

        // 첨부파일 캡슐에 연결
        attachFiles(member.getMemberId(), saved, capsuleCreate.attachmentIds());

        return CapsuleCreateResponseDTO.from(saved);
    }

    /**
     * 비공개 캡슐 생성 - 통합 진입점
     */
    @Transactional
    public SecretCapsuleCreateResponseDTO createPrivateCapsule(SecretCapsuleCreateRequestDTO requestDTO) {
        String recipientPhone = requestDTO.recipientPhone();
        String capsulePassword = requestDTO.capsulePassword();

        boolean hasPhone = recipientPhone != null && !recipientPhone.isBlank();
        boolean hasPassword = capsulePassword != null && !capsulePassword.isBlank();

        if (!hasPhone && !hasPassword) {
            throw new BusinessException(ErrorCode.CAPSULE_NOT_CREATE);
        }
        if (hasPhone && hasPassword) {
            throw new BusinessException(ErrorCode.CAPSULE_NOT_CREATE);
        }

        if (hasPhone) {
            return privateCapsulePhone(requestDTO, recipientPhone);
        } else {
            return privateCapsulePassword(requestDTO, capsulePassword);
        }
    }

    /**
     * 비공개 캡슐 생성 - URL + 비밀번호 조회
     */

    private SecretCapsuleCreateResponseDTO privateCapsulePassword(SecretCapsuleCreateRequestDTO capsuleCreate, String password) {

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Capsule secretCapsule = capsuleCreate.toEntity();

        // 수신자 닉네임 null 방지 ("" 허용)
        if (secretCapsule.getReceiverNickname() == null) {
            throw new BusinessException(ErrorCode.RECEIVERNICKNAME_IS_REQUIRED);
        }

        // ✅ moderation (저장 직전 / flagged면 예외 / PASS는 저장 안 함)
        capsuleModerationService.validateCapsuleText(
                member.getMemberId(),
                ModerationActionType.CAPSULE_CREATE,
                secretCapsule.getTitle(),
                secretCapsule.getContent(),
                secretCapsule.getReceiverNickname(),
                secretCapsule.getLocationName(),
                secretCapsule.getAddress()
        );

        secretCapsule.setUuid(setUUID());
        secretCapsule.setCapPassword(phoneCrypto.hash(password));
        secretCapsule.setMemberId(member);

        isCapsuleProfileIncomplete(secretCapsule);

        // URL+비밀번호 방식은 보호=0
        secretCapsule.setProtected(0);

        Capsule saved = capsuleRepository.save(secretCapsule);
        // 첨부파일 캡슐에 연결
        attachFiles(member.getMemberId(), saved, capsuleCreate.attachmentIds());

        String url = domain + "/" + saved.getUuid();
        return SecretCapsuleCreateResponseDTO.from(saved, url, password);
    }

    /**
     * 비공개 캡슐 생성 - 전화번호 조회
     * - 수신자 전화번호가 "회원"이면 보호=1 (recipient 테이블 저장)
     * - "비회원"이면 보호=0 + 비밀번호 생성
     */

    private SecretCapsuleCreateResponseDTO privateCapsulePhone(SecretCapsuleCreateRequestDTO capsuleCreate, String receiveTel) {

        Capsule capsule = capsuleCreate.toEntity();
        capsule.setUuid(setUUID());

        if (capsule.getReceiverNickname() == null) {
            throw new BusinessException(ErrorCode.RECEIVERNICKNAME_IS_REQUIRED);
        }

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // ✅ moderation (저장 직전 / flagged면 예외 / PASS는 저장 안 함)
        capsuleModerationService.validateCapsuleText(
                member.getMemberId(),
                ModerationActionType.CAPSULE_CREATE,
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getReceiverNickname(),
                capsule.getLocationName(),
                capsule.getAddress()
        );

        boolean isRecipientMember = memberRepository.existsByPhoneHash(phoneCrypto.hash(receiveTel));

        if (isRecipientMember) { // 회원 수신자

            capsule.setMemberId(member);

            isCapsuleProfileIncomplete(capsule);

            capsule.setProtected(1); // ✅ 보호=1
            Capsule saved = capsuleRepository.save(capsule);

            CapsuleRecipient recipientRecord = CapsuleRecipient.builder()
                    .capsuleId(saved)
                    .recipientName(capsuleCreate.nickname())
                    .recipientPhone(receiveTel)
                    .recipientPhoneHash(phoneCrypto.hash(receiveTel))
                    .isSenderSelf(0)
                    .build();

            recipientRepository.save(recipientRecord);
            // 첨부파일 캡슐에 연결
            attachFiles(member.getMemberId(), saved, capsuleCreate.attachmentIds());

            String url = domain + "/" + saved.getUuid();
            smsNotificaationService.sendCapsuleCreatedNotification(receiveTel,member.getName(),capsule.getTitle());
            return SecretCapsuleCreateResponseDTO.from(saved, url, null);

        } else { // 비회원 수신자

            String capsulePW = generatePassword();
            capsule.setCapPassword(phoneCrypto.hash(capsulePW));
            capsule.setMemberId(member);

            isCapsuleProfileIncomplete(capsule);

            capsule.setProtected(0); // ✅ 미보호=0
            Capsule saved = capsuleRepository.save(capsule);
            // 첨부파일 캡슐에 연결
            attachFiles(member.getMemberId(), saved, capsuleCreate.attachmentIds());

            String url = domain + "/" + saved.getUuid();
            return SecretCapsuleCreateResponseDTO.from(saved, url, capsulePW);
        }
    }

    /**
     * 비공개 캡슐 - 나에게 보내는 캡슐 (보호=1)
     */
    @Transactional
    public SecretCapsuleCreateResponseDTO capsuleToMe(SecretCapsuleCreateRequestDTO requestDTO, String encryptedPhone, String phoneHash) {
        Capsule capsule = requestDTO.toEntity();

        if (capsule.getReceiverNickname() == null) {
            throw new BusinessException(ErrorCode.RECEIVERNICKNAME_IS_REQUIRED);
        }

        Member member = memberRepository.findById(requestDTO.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // ✅ moderation (저장 직전 / flagged면 예외 / PASS는 저장 안 함)
        capsuleModerationService.validateCapsuleText(
                member.getMemberId(),
                ModerationActionType.CAPSULE_CREATE,
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getReceiverNickname(),
                capsule.getLocationName(),
                capsule.getAddress()
        );

        capsule.setProtected(1);
        capsule.setUuid(setUUID());
        capsule.setMemberId(member);

        isCapsuleProfileIncomplete(capsule);

        Capsule saved = capsuleRepository.save(capsule);
        // 첨부파일 캡슐에 연결
        attachFiles(member.getMemberId(), saved, requestDTO.attachmentIds());

        CapsuleRecipient recipientRecord = CapsuleRecipient.builder()
                .capsuleId(saved)
                .recipientName(requestDTO.nickname())
                .recipientPhone(encryptedPhone)
                .recipientPhoneHash(phoneHash)
                .isSenderSelf(1)
                .build();

        recipientRepository.save(recipientRecord);

        return SecretCapsuleCreateResponseDTO.from(saved, null, null);
    }

    /**
     * 캡슐 수정
     * - 열람 전(조회수=0)만 수정 가능
     * - moderation flagged이면 수정 자체를 막고(CPS011) payload로 위반 필드/카테고리 내려줌
     *
     * ✅ 수정은 capsuleId가 이미 있으므로,
     *    실패(예외) 때 payload의 auditId를 뽑아서 attach해두면 관리자 추적이 좋아짐
     */
    @Transactional
    public CapsuleUpdateResponseDTO updateCapsule(Long capsuleId, CapsuleUpdateRequestDTO updateDTO) {

        if (capsuleRepository.findCurrentViewCountByCapsuleId(capsuleId) > 0) {
            throw new BusinessException(ErrorCode.CAPSULE_NOT_UPDATE);
        }

        Capsule targetCapsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        String nextTitle = (updateDTO.title() != null) ? updateDTO.title() : targetCapsule.getTitle();
        String nextContent = (updateDTO.content() != null) ? updateDTO.content() : targetCapsule.getContent();

        Long actorId = null;
        if (targetCapsule.getMemberId() != null) {
            actorId = targetCapsule.getMemberId().getMemberId();
        }

        // ✅ 수정 저장 전 moderation (flagged/error면 예외)
        try {
            capsuleModerationService.validateCapsuleText(
                    actorId,
                    ModerationActionType.CAPSULE_UPDATE,
                    nextTitle,
                    nextContent,
                    targetCapsule.getReceiverNickname(),
                    targetCapsule.getLocationName(),
                    targetCapsule.getAddress()
            );
        } catch (BusinessException e) {
            // 실패 로그(FLAGGED/ERROR)는 이미 저장되었으니, auditId를 뽑아서 capsuleId만 붙여준다
            Long auditId = extractAuditId(e.getData());
            capsuleModerationService.attachCapsuleId(auditId, targetCapsule.getCapsuleId());
            throw e;
        }

        if (updateDTO.title() != null) {
            targetCapsule.setTitle(updateDTO.title());
        }
        if (updateDTO.content() != null) {
            targetCapsule.setContent(updateDTO.content());
        }

        Capsule saved = capsuleRepository.save(targetCapsule);
        // 첨부파일 캡슐에 연결
        attachFiles(actorId, saved, updateDTO.attachmentIds());
        return CapsuleUpdateResponseDTO.from(saved);
    }

    /**
     * 캡슐 삭제 - 수신자 삭제
     */
    @Transactional
    public CapsuleDeleteResponseDTO receiverDelete(Long capsuleId, String phoneHash) {

        Optional<CapsuleRecipient> privateOpt =
                recipientRepository.findByCapsuleId_CapsuleIdAndRecipientPhoneHash(capsuleId, phoneHash);

        if (privateOpt.isPresent()) {
            CapsuleRecipient privateCapsule = privateOpt.get();

            // deletedAt 갱신
            privateCapsule.markDeleted();

            // 트랜잭션으로 인해 삭제 후 다시 DB 저장 문제 해결을 위해 삭제
            // recipientRepository.save(privateCapsule);

            return new CapsuleDeleteResponseDTO(
                    capsuleId,
                    capsuleId + "번 캡슐이 삭제 되었습니다."
            );
        }

        Optional<PublicCapsuleRecipient> publicOps =
                publicRecipientRepository.findByCapsuleIdAndPhoneHash(capsuleId, phoneHash);

        if (publicOps.isPresent()) {
            PublicCapsuleRecipient publicCapsule = publicOps.get();

            // deletedAt 갱신
            publicCapsule.markDeleted();

            // 트랜잭션으로 인해 삭제 후 다시 DB 저장 문제 해결을 위해 삭제
            // publicRecipientRepository.save(publicCapsule);

            return new CapsuleDeleteResponseDTO(
                    capsuleId,
                    capsuleId + "번 캡슐이 삭제 되었습니다."
            );
        } else {
            throw new BusinessException(ErrorCode.CAPSULE_NOT_FOUND);
        }
    }

    /**
     * 캡슐 삭제 - 발신자 삭제
     */
    @Transactional
    public CapsuleDeleteResponseDTO senderDelete(Long capsuleId, Long memberId) {

        Capsule capsule = capsuleRepository.findByCapsuleIdAndMemberId_MemberId(capsuleId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        capsule.markDeleted();
        capsule.setIsDeleted(1);

        // 관련된 첨부파일 모두 삭제 처리
        List<CapsuleAttachment> used = capsuleAttachmentRepository
                .findAllByCapsule_CapsuleIdAndStatus(capsuleId, CapsuleAttachmentStatus.USED);

        for (CapsuleAttachment a : used) {
            a.markDeleted(); // status=DELETED, deletedAt=now()
        }
        capsuleAttachmentRepository.saveAll(used);

        return new CapsuleDeleteResponseDTO(
                capsuleId,
                capsuleId + "번 캡슐이 삭제 되었습니다."
        );
    }

    /**
     * BusinessException payload에서 auditId 뽑기
     * - FLAGGED: CapsuleModerationBlockedPayload
     * - ERROR : Map payload { auditId, reason }
     */
    private Long extractAuditId(Object data) {
        if (data == null) return null;

        if (data instanceof CapsuleModerationBlockedPayload payload) {
            return payload.getAuditId();
        }

        if (data instanceof Map<?, ?> map) {
            Object v = map.get("auditId");
            if (v instanceof Number n) return n.longValue();
        }

        return null;
    }

    // 첨부파일 캡슐에 연결
    private List<Long> attachFiles(Long memberId, Capsule capsule, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return List.of();

        List<CapsuleAttachment> attachments =
                capsuleAttachmentRepository.findAllById(attachmentIds);

        if (attachments.size() != attachmentIds.size()) {
            throw new BusinessException(ErrorCode.CAPSULE_FILES_NOT_FOUND);
        }
        for (CapsuleAttachment a : attachments) {
            if (!a.getUploaderId().equals(memberId)) {
                throw new BusinessException(ErrorCode.CAPSULE_FILE_ATTACH_FORBIDDEN);
            }
            if (a.getStatus() != CapsuleAttachmentStatus.TEMP) {
                throw new BusinessException(ErrorCode.CAPSULE_FILE_ATTACH_INVALID_STATUS);
            }
            a.attachToCapsule(capsule); // capsule 세팅 + USED
        }
        capsuleAttachmentRepository.saveAll(attachments);

        return attachments.stream()
                .map(CapsuleAttachment::getId)
                .toList();
    }

}
