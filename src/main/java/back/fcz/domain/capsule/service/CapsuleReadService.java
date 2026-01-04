package back.fcz.domain.capsule.service;

import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentViewResponse;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleReadResponse;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.*;
import back.fcz.domain.member.dto.response.MemberDetailResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.member.service.MemberService;
import back.fcz.domain.sanction.constant.SanctionConstants;
import back.fcz.domain.sanction.service.MonitoringService;
import back.fcz.domain.unlock.dto.UnlockValidationResult;
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
    private final MonitoringService monitoringService;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final PresignedUrlProvider presignedUrlProvider;
    private final CapsuleOpenLogService capsuleOpenLogService;
    private final SanctionConstants sanctionConstants;

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

        // 성공 기록이 있는지 확인
        boolean hasSuccessfullyViewed = capsuleOpenLogRepository
                .existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                        capsule.getCapsuleId(),
                        currentMemberId,
                        CapsuleOpenStatus.SUCCESS
                );

        if (hasSuccessfullyViewed) {
            log.info("이전 성공 기록 존재, 재조회 - 로그 기록 및 이상 감지만 수행");
            return handlePublicReview(capsule, requestDto, currentMemberId);
        }

        log.info("이전 성공 기록 미존재, 첫 조회 - 검증 시작");

        // 시간/위치 조건 검증 + 이상 감지
        UnlockValidationResult validationResult = unlockService.validateTimeAndLocationConditions(
                capsule,
                requestDto.unlockAt(),
                requestDto.locationLat(),
                requestDto.locationLng(),
                requestDto.serverTime(),
                currentMemberId,
                requestDto.ipAddress()
        );

        // 조건 검증 실패 시
        if(!validationResult.isSuccess()) {
            CapsuleOpenStatus status = determineOpenStatus(validationResult, capsule.getUnlockType());

            // 로그 생성
            CapsuleOpenLog openLog = createOpenLog(
                    capsule,
                    requestDto,
                    status,
                    currentMemberId,
                    "MEMBER"
            );
            capsuleOpenLogService.saveLogInNewTransaction(openLog);

            detectAndHandleAnomaly(openLog, validationResult, currentMemberId, requestDto.ipAddress());

            if (validationResult.hasAnomaly()) {
                throwAnomalyException(validationResult, currentMemberId);
            }

            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        // 조건 검증 성공 시
        if (firstComeService.hasFirstComeLimit(capsule)) {
            log.info("선착순 제한 있음 - maxViewCount: {}", capsule.getMaxViewCount());

            boolean isNewView = firstComeService.tryIncrementViewCountAndSaveRecipient(
                    capsule.getCapsuleId(),
                    currentMemberId,
                    requestDto
            );

            log.info("선착순 검증 및 저장 완료");
            return readPublicCapsule(capsule, requestDto, !isNewView);
        } else {
            log.info("선착순 없음 - 바로 저장");

            // 선착순 없는 경우도 FirstComeService로
            firstComeService.saveRecipientWithoutFirstCome(
                    capsule.getCapsuleId(),
                    currentMemberId,
                    requestDto
            );

            log.info("=== 공개 캡슐 로직 종료 ===");
            return readPublicCapsule(capsule, requestDto, false);
        }
    }

    // 공개 캡슐 재조회
    private CapsuleConditionResponseDTO handlePublicReview(
            Capsule capsule,
            CapsuleConditionRequestDTO requestDto,
            Long currentMemberId
    ) {
        log.info("공개 캡슐 재조회 - capsuleId: {}, memberId: {}", capsule.getCapsuleId(), currentMemberId);

        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                currentMemberId,
                "MEMBER"
        );
        capsuleOpenLogService.saveLogInNewTransaction(openLog);

        log.info("공개 캡슐 재조회 완료");
        return readPublicCapsule(capsule, requestDto, true);
    }

    //개인 캡슐
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

        InServerMemberResponse user = currentUserContext.getCurrentUser();
        MemberDetailResponse response = memberService.getDetailMe(user);
        String phoneNumber = response.phoneNumber();
        Long currentMemberId = user.memberId();

        CapsuleRecipient recipient = capsuleRecipientRepository
                .findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                .orElse(null);

        if (recipient == null || !phoneCrypto.verifyHash(phoneNumber, recipient.getRecipientPhoneHash())) {
            log.warn("수신자가 아닌 회원의 접근 시도 - memberId: {}", user.memberId());

            CapsuleOpenLog openLog = createOpenLog(
                    capsule,
                    requestDto,
                    CapsuleOpenStatus.FAIL_PERMISSION,
                    currentMemberId,
                    "MEMBER"
            );
            capsuleOpenLogService.saveLogInNewTransaction(openLog);

            if (recipient == null) {
                throw new BusinessException(ErrorCode.RECIPIENT_NOT_FOUND);
            } else {
                throw new BusinessException(ErrorCode.CAPSULE_NOT_RECEIVER);
            }
        }

        log.info("수신자 본인 확인 완료");

        // 성공 기록이 있는지 확인
        boolean hasSuccessfullyViewed = capsuleOpenLogRepository
                .existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                        capsule.getCapsuleId(),
                        currentMemberId,
                        CapsuleOpenStatus.SUCCESS
                );

        if (hasSuccessfullyViewed) {
            log.info("이전 성공 기록 존재, 재조회 - 로그 기록 및 이상 감지만 수행");
            return handleProtectedReview(capsule, requestDto, currentMemberId, recipient);
        }

        log.info("이전 성공 기록 미존재, 첫 조회 - 검증 시작");

        // 조건 검증 + 이상 감지
        UnlockValidationResult validationResult = unlockService.validateUnlockConditionsForPrivate(
                capsule,
                requestDto.unlockAt(),
                requestDto.locationLat(),
                requestDto.locationLng(),
                requestDto.serverTime(),
                currentMemberId,
                requestDto.ipAddress()
        );

        // 로그 생성
        CapsuleOpenStatus status = determineOpenStatus(validationResult, capsule.getUnlockType());
        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                status,
                currentMemberId,
                "MEMBER"
        );
        capsuleOpenLogService.saveLogInNewTransaction(openLog);

        // 실패 시 이상 탐지 및 제재 처리
        if (!validationResult.isSuccess()) {
            detectAndHandleAnomaly(openLog, validationResult, currentMemberId, requestDto.ipAddress());

            if (validationResult.hasAnomaly()) {
                throwAnomalyException(validationResult, currentMemberId);
            }

            log.warn("조건 미충족 또는 이상 활동 감지");
            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        log.info("시간/위치 조건 통과 - 캡슐 조회 허용");
        return readMemberCapsule(capsule, requestDto, true, recipient);
    }

    // 보호된 캡슐 재조회
    private CapsuleConditionResponseDTO handleProtectedReview(
            Capsule capsule,
            CapsuleConditionRequestDTO requestDto,
            Long currentMemberId,
            CapsuleRecipient recipient
    ) {
        log.info("보호된 캡슐 재조회 - capsuleId: {}, memberId: {}", capsule.getCapsuleId(), currentMemberId);

        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                currentMemberId,
                "MEMBER"
        );
        capsuleOpenLogService.saveLogInNewTransaction(openLog);

        log.info("보호된 캡슐 재조회 완료");
        return readMemberCapsule(capsule, requestDto, false, recipient);
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

            // 로그인 상태 확인
            boolean isLoggedIn = isUserLoggedIn();
            Long memberId = isLoggedIn ? currentUserContext.getCurrentMemberId() : null;
            String viewerType = isLoggedIn ? "MEMBER" : "GUEST";

            // 로그 생성 및 저장
            CapsuleOpenLog openLog = createOpenLog(
                    capsule,
                    requestDto,
                    CapsuleOpenStatus.FAIL_PASSWORD,
                    memberId,
                    viewerType
            );
            capsuleOpenLogService.saveLogInNewTransaction(openLog);

            throw new BusinessException(ErrorCode.CAPSULE_PASSWORD_NOT_MATCH);
        }

        log.info("비밀번호 검증 통과");

        boolean isLoggedIn = isUserLoggedIn();
        Long memberId = isLoggedIn ? currentUserContext.getCurrentMemberId() : null;
        String viewerType = isLoggedIn ? "MEMBER" : "GUEST";

        boolean hasAlreadyViewed = hasAlreadyOpenedUnprotected(capsule.getCapsuleId(), memberId, requestDto.ipAddress());

        if (hasAlreadyViewed) {
            log.info("재조회 - 로그 기록 및 이상 감지, 로그인 여부: {}", isLoggedIn);
            return handleUnprotectedReview(capsule, requestDto, memberId, viewerType);
        }

        log.info("첫 조회 - 조건 검증 시작");

        // 조건 검증 + 이상 감지
        UnlockValidationResult validationResult = unlockService.validateUnlockConditionsForPrivate(
                capsule,
                requestDto.unlockAt(),
                requestDto.locationLat(),
                requestDto.locationLng(),
                requestDto.serverTime(),
                memberId,
                requestDto.ipAddress()
        );

        // 로그 생성
        CapsuleOpenStatus status = determineOpenStatus(validationResult, capsule.getUnlockType());
        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                status,
                memberId,
                viewerType
        );
        capsuleOpenLogService.saveLogInNewTransaction(openLog);

        // 실패 시 이상 탐지 및 제재 처리
        if (!validationResult.isSuccess()) {
            detectAndHandleAnomaly(openLog, validationResult, memberId, requestDto.ipAddress());

            if (validationResult.hasAnomaly()) {
                throwAnomalyException(validationResult, memberId);
            }

            log.warn("조건 미충족 또는 이상 활동 감지");
            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        detectAndHandleAnomaly(openLog, validationResult, memberId, requestDto.ipAddress());
        if (validationResult.hasAnomaly()) {
            throwAnomalyException(validationResult, memberId);
        }

        log.info("시간/위치 조건 통과 - 캡슐 읽기 허용, 로그인 여부: {}", isLoggedIn);

        if (isLoggedIn) {
            return readMemberCapsuleWithoutRecipient(capsule, requestDto, true);
        } else {
            return readCapsuleAsGuest(capsule, requestDto, true);
        }
    }

    // 비보호 캡슐 재조회
    private CapsuleConditionResponseDTO handleUnprotectedReview(
            Capsule capsule,
            CapsuleConditionRequestDTO requestDto,
            Long memberId,
            String viewerType
    ) {
        log.info("비보호 캡슐 재조회 - capsuleId: {}, memberId: {}", capsule.getCapsuleId(), memberId);

        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                memberId,
                viewerType
        );
        capsuleOpenLogService.saveLogInNewTransaction(openLog);
        log.info("비보호 캡슐 재조회 완료");

        boolean isLoggedIn = (memberId != null);
        if (isLoggedIn) {
            return readMemberCapsuleWithoutRecipient(capsule, requestDto, false);
        } else {
            return readCapsuleAsGuest(capsule, requestDto, false);
        }
    }

    // 비보호 캡슐 재조회 여부 (memberId 또는 IP 기반으로 성공 여부 확인)
    private boolean hasAlreadyOpenedUnprotected(Long capsuleId, Long memberId, String ipAddress) {
        // 회원인 경우
        if (memberId != null) {
            return capsuleOpenLogRepository
                    .existsByCapsuleId_CapsuleIdAndMemberId_MemberIdAndStatus(
                            capsuleId,
                            memberId,
                            CapsuleOpenStatus.SUCCESS
                    );
        }

        // 비회원인 경우 (IP 기반)
        if (ipAddress != null && !ipAddress.equals("UNKNOWN")) {
            return capsuleOpenLogRepository
                    .existsByCapsuleId_CapsuleIdAndIpAddressAndStatus(
                            capsuleId,
                            ipAddress,
                            CapsuleOpenStatus.SUCCESS
                    );
        }

        return false;
    }

    //공개 캡슐 읽기
    public CapsuleConditionResponseDTO readPublicCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto, boolean viewStatus) {
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

        // 첫 조회일 때만 조회수 증가 및 unlockedAt 설정
        if (shouldIncrement) {
            if (recipient.getUnlockedAt() == null) {
                recipient.setUnlockedAt(requestDto.unlockAt());
                capsuleRecipientRepository.save(recipient);
            }

            capsuleRepository.incrementViewCount(capsule.getCapsuleId());
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

        // 첫 조회일 때만 조회수 증가
        if (shouldIncrement) {
            capsuleRepository.incrementViewCount(capsule.getCapsuleId());
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

        // 처음 조회하면 조회수 증가
        if (shouldIncrement) {
            capsuleRepository.incrementViewCount(capsule.getCapsuleId());
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

    @Transactional(readOnly = true)
    public CapsuleReadResponse existedPassword(String request){
        Capsule capsule = capsuleRepository.findByUuid(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        if(capsule.getCapPassword()==null){
            return CapsuleReadResponse.from(capsule.getCapsuleId(), capsule.getIsProtected(), false);
        }else{
            return CapsuleReadResponse.from(capsule.getCapsuleId(), capsule.getIsProtected(),true);
        }
    }

    // CapsuleOpenLog 생성 메서드
    private CapsuleOpenLog createOpenLog(
            Capsule capsule,
            CapsuleConditionRequestDTO requestDto,
            CapsuleOpenStatus status,
            Long memberId,
            String viewerType
    ) {
        Member member = null;
        if(memberId != null) {
            member = memberRepository.findById(memberId).orElse(null);
        }

        return CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(member)
                .viewerType(viewerType)
                .status(status)
                .anomalyType(AnomalyType.NONE)
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(requestDto.userAgent())
                .ipAddress(requestDto.ipAddress())
                .build();
    }

    // 이상 활동 감지 및 제재 처리
    private void detectAndHandleAnomaly(
            CapsuleOpenLog openLog,
            UnlockValidationResult validationResult,
            Long memberId,
            String ipAddress
    ) {
        if (!validationResult.hasAnomaly()) {
            return;
        }

        // 로그에 이상 유형 기록
        openLog.markAsAnomaly(validationResult.getAnomalyType());
        capsuleOpenLogService.saveLogInNewTransaction(openLog);

        log.info("이상 활동 감지: anomalyType={}, score={}, memberId={}, ip={}",
                validationResult.getAnomalyType(),
                validationResult.getSuspicionScore(),
                memberId,
                ipAddress);

        // 의심 점수 증가
        if (memberId != null) {
            monitoringService.incrementSuspicionScore(memberId, validationResult.getSuspicionScore());
        } else if (ipAddress != null && !ipAddress.equals("UNKNOWN")) {
            monitoringService.incrementSuspicionScoreByIp(ipAddress, validationResult.getSuspicionScore());
        }
    }

    // 이상 유형에 따라 예외 발생
    private void throwAnomalyException(UnlockValidationResult validationResult, Long memberId) {
        AnomalyType anomalyType = validationResult.getAnomalyType();

        log.error("이상 활동으로 인한 접근 차단: memberId={}, anomalyType={}, score={}",
                memberId, anomalyType, validationResult.getSuspicionScore());

        switch (anomalyType) {
            case IMPOSSIBLE_MOVEMENT:
                throw new BusinessException(ErrorCode.GPS_SPOOFING_SUSPECTED);
            case TIME_MANIPULATION:
                throw new BusinessException(ErrorCode.ANOMALY_DETECTED, "시간 정보 조작이 의심됩니다.");
            case RAPID_RETRY:
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
            case LOCATION_RETRY:
            case SUSPICIOUS_PATTERN:
                throw new BusinessException(ErrorCode.SUSPICIOUS_ACCESS_PATTERN);
            default:
                break;
        }
    }

    // 상태 판별 헬퍼 메서드
    private CapsuleOpenStatus determineOpenStatus(UnlockValidationResult validationResult, String unlockType) {
        // 이상 감지된 경우
        if (validationResult.hasAnomaly()) {
            return CapsuleOpenStatus.SUSPICIOUS;
        }

        // 조건 충족된 경우
        if (validationResult.isSuccess()) {
            return CapsuleOpenStatus.SUCCESS;
        }

        // 조건 미충족 - unlockType에 따라 세분화
        return switch (unlockType) {
            case "TIME" -> CapsuleOpenStatus.FAIL_TIME;
            case "LOCATION" -> CapsuleOpenStatus.FAIL_LOCATION;
            case "TIME_AND_LOCATION" -> CapsuleOpenStatus.FAIL_BOTH;
            default -> CapsuleOpenStatus.FAIL_BOTH;
        };
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
