package back.fcz.domain.capsule.service;

import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentViewResponse;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleReadResponse;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.*;
import back.fcz.domain.member.dto.response.MemberDetailResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.member.service.MemberService;
import back.fcz.domain.unlock.service.FirstComeService;
import back.fcz.domain.unlock.service.UnlockService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.infra.storage.PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapsuleReadService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final PhoneCrypto phoneCrypto;
    private final UnlockService unlockService;
    private final FirstComeService firstComeService;
    private final MemberRepository memberRepository;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final MemberService memberService;
    private final CurrentUserContext currentUserContext;
    private final BookmarkRepository bookmarkRepository;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final PresignedUrlProvider presignedUrlProvider;

    public CapsuleConditionResponseDTO capsuleRead(Long capsuleId){
        //자신이 작성한 캡슐이면 검증 없이 읽기
        Capsule capsule = capsuleRepository.findById(capsuleId).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        Long currentMemberId = currentUserContext.getCurrentMemberId();

        //본인이 작성한 캡슐인지 확인
        if(!currentMemberId.equals(capsule.getMemberId().getMemberId())){
            throw new BusinessException(ErrorCode.NOT_SELF_CAPSULE);
        }
        var attachments = buildAttachmentViews(capsule.getCapsuleId());
        if(capsule.getVisibility().equals("PUBLIC")){
            boolean viewStatus = publicCapsuleRecipientRepository
                    .existsByCapsuleId_CapsuleIdAndMemberId(capsule.getCapsuleId(), currentMemberId);

            return CapsuleConditionResponseDTO.from(capsule, viewStatus, false, attachments);
        }else{
            return CapsuleConditionResponseDTO.from(capsule, attachments);
        }
    }

    //조건 확인하고 검증됐다면 읽기
    @Transactional
    public CapsuleConditionResponseDTO conditionAndRead(CapsuleConditionRequestDTO requestDto) {
        Capsule capsule = capsuleRepository.findById(requestDto.capsuleId()).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        // 공개인지 비공개인지
        if(capsule.getVisibility().equals("PUBLIC")){
            // PUBLIC 캡슐 로직
            log.info("공개 캡슐 로직 진입 - capsuleId: {}", capsule.getCapsuleId());
            return publicCapsuleLogic(capsule, requestDto);
        }else{
            // PRIVATE와 SELF 모두 개인 캡슐 로직으로 동일하게 처리
            return privateCapsuleLogic(capsule, requestDto);
        }
    }

    //공개 캡슐
    public CapsuleConditionResponseDTO publicCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        log.info("=== 공개 캡슐 로직 시작 - capsuleId: {} ===", capsule.getCapsuleId());

        // 공개 캡슐은 회원만 조회 가능 - 로그인 체크
        if (!isUserLoggedIn()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        Long currentMemberId = currentUserContext.getCurrentMemberId();
        log.info("로그인 회원 - memberId: {}", currentMemberId);

        // 시간/위치 조건 검증
        boolean conditionMet = unlockService.validateTimeAndLocationConditions(
                capsule, requestDto.unlockAt(), requestDto.locationLat(), requestDto.locationLng()
        );

        if (!conditionMet) {
            log.warn("시간/위치 조건 미충족");
            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        log.info("시간/위치 조건 통과");

        boolean isNewView = firstComeService.tryIncrementViewCountAndSaveRecipient(
                capsule.getCapsuleId(),
                currentMemberId,
                requestDto.unlockAt()
        );

        log.info("공개 캡슐 조회 처리 완료. isNewView={}", isNewView);
        log.info("=== 공개 캡슐 로직 종료 ===");

        // viewStatus: true = 재조회, false = 첫 조회
        return readPublicCapsule(capsule, requestDto, !isNewView);
    }

    //개인 캡슐
    @Transactional
    public CapsuleConditionResponseDTO privateCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        //전화번호 기반인지 url+비번 기반인지를 먼저 확인하고 조회 횟수를 검증할것
        boolean hasPassword = !(requestDto.password() == null || requestDto.password().isBlank());
        boolean isLoggedIn = isUserLoggedIn();

        log.info("개인 캡슐 검증 시작 - hasPassword: {}, isLoggedIn: {}, isProtected: {}",
                hasPassword, isLoggedIn, capsule.getIsProtected());

        if (capsule.getIsProtected() == 1) {
            // JWT 인증 필수 (전화번호 전송 방식 OR URL+비밀번호에서 저장 버튼 누른 경우)
            log.info("isProtected=1 캡슐 - JWT 인증 필수");
            return handleProtectedCapsule(capsule, requestDto);
        } else {
            // 비밀번호 인증 (URL+비밀번호 방식, 아직 저장 안 함)
            log.info("isProtected=0 캡슐 - 비밀번호 인증");
            return handleUnprotectedCapsule(capsule, requestDto);
        }
    }

    // isProtected = 1 처리: JWT 인증 + 전화번호 해시 검증
    private CapsuleConditionResponseDTO handleProtectedCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        log.info("보호된 캡슐 접근 처리 - capsuleId: {}", capsule.getCapsuleId());

        if (!isUserLoggedIn()) {
            log.warn("비로그인 상태로 isProtected=1 캡슐 접근 시도");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        CapsuleRecipient recipient = capsuleRecipientRepository
                .findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPIENT_NOT_FOUND));

        InServerMemberResponse user = currentUserContext.getCurrentUser();
        MemberDetailResponse response = memberService.getDetailMe(user);
        String phoneNumber = response.phoneNumber();

        if (!phoneCrypto.verifyHash(phoneNumber, recipient.getRecipientPhoneHash())) {
            log.warn("수신자가 아닌 회원의 접근 시도 - memberId: {}", user.memberId());
            throw new BusinessException(ErrorCode.CAPSULE_NOT_RECEIVER);
        }

        log.info("수신자 본인 확인 완료");

        if(capsule.getCurrentViewCount() > 0) {
            log.info("재조회 - 조건 검증 생략");
            return readMemberCapsule(capsule, requestDto, false, recipient);
        }

        log.info("첫 조회 - 조건 검증 시작");
        boolean conditionMet = unlockService.validateUnlockConditionsForPrivate(
                capsule,
                requestDto.unlockAt(),
                requestDto.locationLat(),
                requestDto.locationLng()
        );

        if (!conditionMet) {
            log.warn("시간/위치 조건 미충족");
            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        log.info("시간/위치 조건 통과 - 캡슐 조회 허용");
        return readMemberCapsule(capsule, requestDto, true, recipient);
    }

    // isProtected=0 처리: 비밀번호 검증 (로그인 여부는 로그 타입에만 영향)
    private CapsuleConditionResponseDTO handleUnprotectedCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        log.info("비보호 캡슐 접근 처리 - capsuleId: {}", capsule.getCapsuleId());

        if (requestDto.password() == null || requestDto.password().isBlank()) {
            log.warn("비밀번호 미입력");
            throw new BusinessException(ErrorCode.CAPSULE_PASSWORD_REQUIRED);
        }

        if (!phoneCrypto.verifyHash(requestDto.password(), capsule.getCapPassword())) {
            log.warn("비밀번호 불일치");
            throw new BusinessException(ErrorCode.CAPSULE_PASSWORD_NOT_MATCH);
        }

        log.info("비밀번호 검증 통과");

        boolean isLoggedIn = isUserLoggedIn();

        if (capsule.getCurrentViewCount() > 0) {
            log.info("재조회 - 조건 검증 생략, 로그인 여부: {}", isLoggedIn);
            if (isLoggedIn) {
                return readMemberCapsuleWithoutRecipient(capsule, requestDto, false);
            } else {
                return readCapsuleAsGuest(capsule, requestDto, false);
            }
        }

        log.info("첫 조회 - 조건 검증 시작");
        boolean conditionMet = unlockService.validateUnlockConditionsForPrivate(
                capsule,
                requestDto.unlockAt(),
                requestDto.locationLat(),
                requestDto.locationLng()
        );

        if (!conditionMet) {
            log.warn("시간/위치 조건 미충족");
            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        log.info("시간/위치 조건 통과 - 캡슐 읽기 허용, 로그인 여부: {}", isLoggedIn);

        if (isLoggedIn) {
            return readMemberCapsuleWithoutRecipient(capsule, requestDto, true);
        } else {
            return readCapsuleAsGuest(capsule, requestDto, true);
        }
    }

    //공개 캡슐 읽기
    public CapsuleConditionResponseDTO readPublicCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto, boolean viewStatus) {
        String viewerType = currentUserContext.getCurrentUser().role().getDescription();
        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(null)
                .viewerType(viewerType)
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(null)
                .ipAddress(null)
                .build();
        capsuleOpenLogRepository.save(log);

        Long currentMemberId = currentUserContext.getCurrentMemberId();

        boolean isBookmarked = bookmarkRepository.existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(
                currentMemberId,
                capsule.getCapsuleId()
        );

        var attachments = buildAttachmentViews(capsule.getCapsuleId());
        return CapsuleConditionResponseDTO.from(capsule, viewStatus, isBookmarked, attachments);

    }

    // 개인 캡슐 읽기 - isProtected=1 (CapsuleRecipient 있음)
    public CapsuleConditionResponseDTO readMemberCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto, boolean shouldIncrement, CapsuleRecipient recipient){
        Long currentMemberId = currentUserContext.getCurrentMemberId();
        Member member = memberRepository.findById(currentMemberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(member)
                .viewerType("MEMBER")
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(null)
                .ipAddress(null)
                .build();
        capsuleOpenLogRepository.save(log);

        // 첫 조회일 때만 조회수 증가 및 unlockedAt 설정
        if (shouldIncrement) {
            capsule.increasedViewCount();

            if (recipient.getUnlockedAt() == null) {
                recipient.setUnlockedAt(requestDto.unlockAt());
                capsuleRecipientRepository.save(recipient);
            }
        }

        boolean isBookmarked = bookmarkRepository.existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(
                currentMemberId,
                capsule.getCapsuleId()
        );
        var attachments = buildAttachmentViews(capsule.getCapsuleId());
        return CapsuleConditionResponseDTO.from(capsule, isBookmarked, attachments);
    }

    // 개인 캡슐 읽기 - isProtected=0, 로그인 상태 (CapsuleRecipient 없음)
    private CapsuleConditionResponseDTO readMemberCapsuleWithoutRecipient(Capsule capsule, CapsuleConditionRequestDTO requestDto, boolean shouldIncrement) {
        log.info("회원 캡슐 읽기 (CapsuleRecipient 없음) - capsuleId: {}, shouldIncrement: {}",
                capsule.getCapsuleId(), shouldIncrement);

        Long currentMemberId = currentUserContext.getCurrentMemberId();
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(member)
                .viewerType("MEMBER")
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(null)
                .ipAddress(null)
                .build();
        capsuleOpenLogRepository.save(log);

        // 첫 조회일 때만 조회수 증가
        if (shouldIncrement) {
            capsule.increasedViewCount();
        }

        boolean isBookmarked = bookmarkRepository.existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(
                currentMemberId,
                capsule.getCapsuleId()
        );
        var attachments = buildAttachmentViews(capsule.getCapsuleId());
        return CapsuleConditionResponseDTO.from(capsule, isBookmarked, attachments);
    }

    //개인 캡슐 읽기 - 수신자가 비회원인 경우(로그만 남김)
    public CapsuleConditionResponseDTO readCapsuleAsGuest(Capsule capsule, CapsuleConditionRequestDTO requestDto, boolean shouldIncrement){
        log.info("비회원 캡슐 읽기 - capsuleId: {}, shouldIncrement: {}",
                capsule.getCapsuleId(), shouldIncrement);

        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(null)
                .viewerType("GUEST")
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(null)
                .ipAddress(null)
                .build();
        capsuleOpenLogRepository.save(log);

        // 처음 조회하면 조회수 증가
        if (shouldIncrement) {
            capsule.increasedViewCount();
        }
        var attachments = buildAttachmentViews(capsule.getCapsuleId());
        return CapsuleConditionResponseDTO.from(capsule, attachments);
    }

    // 사용자 로그인 여부
    private boolean isUserLoggedIn() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            Object principal = authentication.getPrincipal();
            return principal instanceof Long;
        } catch (BusinessException e) {
            return false;
        }
    }

    public CapsuleReadResponse existedPassword(String request){
        Capsule capsule = capsuleRepository.findByUuid(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        if(capsule.getCapPassword()==null){
            return CapsuleReadResponse.from(capsule.getCapsuleId(), capsule.getIsProtected(), false);
        }else{
            return CapsuleReadResponse.from(capsule.getCapsuleId(), capsule.getIsProtected(),true);
        }
    }


    // 캡슐 첨부파일 Presigned URL 생성
    private List<CapsuleAttachmentViewResponse> buildAttachmentViews(Long capsuleId) {
        var list = capsuleAttachmentRepository
                .findAllByCapsule_CapsuleIdAndStatus(capsuleId, CapsuleAttachmentStatus.USED);

        return list.stream()
                .map(a -> new CapsuleAttachmentViewResponse(
                        presignedUrlProvider.presignGet(a.getS3Key(), Duration.ofMinutes(15)),
                        a.getId()
                ))
                .toList();
    }
}
