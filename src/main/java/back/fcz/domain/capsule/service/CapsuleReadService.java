package back.fcz.domain.capsule.service;

import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentViewResponse;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleReadResponse;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.*;
import back.fcz.domain.member.dto.response.MemberDetailResponse;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.member.service.MemberService;
import back.fcz.domain.sanction.service.MonitoringService;
import back.fcz.domain.unlock.dto.UnlockValidationResult;
import back.fcz.domain.unlock.service.FirstComeService;
import back.fcz.domain.unlock.service.UnlockService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.infra.storage.PresignedUrlProvider;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapsuleReadService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final PhoneCrypto phoneCrypto;
    private final UnlockService unlockService;
    private final FirstComeService firstComeService;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final MemberService memberService;
    private final CurrentUserContext currentUserContext;
    private final BookmarkRepository bookmarkRepository;
    private final MonitoringService monitoringService;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final PresignedUrlProvider presignedUrlProvider;
    private final CapsuleOpenLogService capsuleOpenLogService;

    // PresignedUrl, 개인 캡슐 조회수 Redis 캐싱
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PRESIGNED_URL_KEY_PREFIX = "presigned:attachment:";
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(14);
    private static final Duration PRESIGNED_URL_VALIDITY = Duration.ofMinutes(15);

    private static final String VIEW_COUNT_KEY_PREFIX = "capsule:view:";
    private static final Duration VIEW_COUNT_TTL = Duration.ofMinutes(30);

    private final ExecutorService s3ExecutorService = Executors.newFixedThreadPool(10);


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
            return publicCapsuleLogic(capsule, requestDto);
        }else{
            // PRIVATE와 SELF 모두 개인 캡슐 로직으로 동일하게 처리
            return privateCapsuleLogic(capsule, requestDto);
        }
    }

    //공개 캡슐
    public CapsuleConditionResponseDTO publicCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {

        // 공개 캡슐은 회원만 조회 가능 - 로그인 체크
        if (!isUserLoggedIn()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        Long currentMemberId = currentUserContext.getCurrentMemberId();

        // 성공 기록이 있는지 확인
        boolean hasSuccessfullyViewed = capsuleOpenLogRepository
                .existsByCapsuleId_CapsuleIdAndMemberIdAndStatus(
                        capsule.getCapsuleId(),
                        currentMemberId,
                        CapsuleOpenStatus.SUCCESS
                );

        if (hasSuccessfullyViewed) {
            return handlePublicReview(capsule, requestDto, currentMemberId);
        }


        // 시간/위치 조건 검증 + 이상 감지
        UnlockValidationResult validationResult = unlockService.validateTimeAndLocationConditions(
                capsule,
                requestDto.serverTime(),
                requestDto.locationLat(),
                requestDto.locationLng(),
                requestDto.unlockAt(),
                currentMemberId,
                requestDto.ipAddress()
        );

        // 조건 검증 실패 시
        if(!validationResult.isSuccess()) {
            CapsuleOpenStatus status = determineOpenStatus(validationResult, capsule.getUnlockType());

            // 로그 생성
            CapsuleOpenLog failLog = createOpenLog(
                    capsule,
                    requestDto,
                    status,
                    currentMemberId,
                    "MEMBER"
            );

            detectAndHandleAnomaly(failLog, validationResult, currentMemberId, requestDto.ipAddress());

            capsuleOpenLogRepository.save(failLog);

            if (validationResult.hasAnomaly()) {
                throwAnomalyException(validationResult, currentMemberId);
            }

            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        // 조건 검증 성공 시
        if (firstComeService.hasFirstComeLimit(capsule)) {

            boolean isNewView = firstComeService.tryIncrementViewCountAndSaveRecipient(
                    capsule.getCapsuleId(),
                    currentMemberId,
                    requestDto
            );

            return readPublicCapsule(capsule, requestDto, !isNewView);
        } else {

            // 로그 생성
            CapsuleOpenLog successLog = createOpenLog(
                    capsule,
                    requestDto,
                    CapsuleOpenStatus.SUCCESS,
                    currentMemberId,
                    "MEMBER"
            );
            capsuleOpenLogRepository.save(successLog);

            // 수신자 정보 저장 + 조회수 증가
            firstComeService.saveRecipientWithoutFirstCome(
                    capsule.getCapsuleId(),
                    currentMemberId,
                    requestDto
            );

            return readPublicCapsule(capsule, requestDto, false);
        }
    }

    // 공개 캡슐 재조회
    private CapsuleConditionResponseDTO handlePublicReview(
            Capsule capsule,
            CapsuleConditionRequestDTO requestDto,
            Long currentMemberId
    ) {
        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                currentMemberId,
                "MEMBER"
        );
        capsuleOpenLogRepository.save(openLog);

        return readPublicCapsule(capsule, requestDto, true);
    }

    //개인 캡슐
    public CapsuleConditionResponseDTO privateCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        //전화번호 기반인지 url+비번 기반인지를 먼저 확인하고 조회 횟수를 검증할것
        boolean hasPassword = !(requestDto.password() == null || requestDto.password().isBlank());
        boolean isLoggedIn = isUserLoggedIn();


        if (capsule.getIsProtected() == 1) {
            // JWT 인증 필수 (전화번호 전송 방식 OR URL+비밀번호에서 저장 버튼 누른 경우)
            return handleProtectedCapsule(capsule, requestDto);
        } else {
            // 비밀번호 인증 (URL+비밀번호 방식, 아직 저장 안 함)
            return handleUnprotectedCapsule(capsule, requestDto);
        }
    }

    // isProtected = 1 처리: JWT 인증 + 전화번호 해시 검증
    private CapsuleConditionResponseDTO handleProtectedCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto) {

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

            CapsuleOpenLog permissionFailLog = createOpenLog(
                    capsule,
                    requestDto,
                    CapsuleOpenStatus.FAIL_PERMISSION,
                    currentMemberId,
                    "MEMBER"
            );
            capsuleOpenLogService.saveLogInNewTransaction(permissionFailLog);

            if (recipient == null) {
                throw new BusinessException(ErrorCode.RECIPIENT_NOT_FOUND);
            } else {
                throw new BusinessException(ErrorCode.CAPSULE_NOT_RECEIVER);
            }
        }


        // 성공 기록이 있는지 확인
        boolean hasSuccessfullyViewed = capsuleOpenLogRepository
                .existsByCapsuleId_CapsuleIdAndMemberIdAndStatus(
                        capsule.getCapsuleId(),
                        currentMemberId,
                        CapsuleOpenStatus.SUCCESS
                );

        if (hasSuccessfullyViewed) {
            return handleProtectedReview(capsule, requestDto, currentMemberId, recipient);
        }


        // 조건 검증 + 이상 감지
        UnlockValidationResult validationResult = unlockService.validateUnlockConditionsForPrivate(
                capsule,
                requestDto.serverTime(),
                requestDto.locationLat(),
                requestDto.locationLng(),
                requestDto.unlockAt(),
                currentMemberId,
                requestDto.ipAddress()
        );

        CapsuleOpenStatus status = determineOpenStatus(validationResult, capsule.getUnlockType());

        // 실패 시 이상 탐지 및 제재 처리
        if (!validationResult.isSuccess()) {
            CapsuleOpenLog failLog = createOpenLog(
                    capsule,
                    requestDto,
                    status,
                    currentMemberId,
                    "MEMBER"
            );

            detectAndHandleAnomaly(failLog, validationResult, currentMemberId, requestDto.ipAddress());

            capsuleOpenLogRepository.save(failLog);

            if (validationResult.hasAnomaly()) {
                throwAnomalyException(validationResult, currentMemberId);
            }

            log.warn("조건 미충족 또는 이상 활동 감지");
            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        CapsuleOpenLog successLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                currentMemberId,
                "MEMBER"
        );
        capsuleOpenLogRepository.save(successLog);

        return readMemberCapsule(capsule, requestDto, true, recipient);
    }

    // 보호된 캡슐 재조회
    private CapsuleConditionResponseDTO handleProtectedReview(
            Capsule capsule,
            CapsuleConditionRequestDTO requestDto,
            Long currentMemberId,
            CapsuleRecipient recipient
    ) {

        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                currentMemberId,
                "MEMBER"
        );
        capsuleOpenLogRepository.save(openLog);

        return readMemberCapsule(capsule, requestDto, false, recipient);
    }

    // isProtected=0 처리: 비밀번호 검증 (로그인 여부는 로그 타입에만 영향)
    private CapsuleConditionResponseDTO handleUnprotectedCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto) {

        if (requestDto.password() == null || requestDto.password().isBlank()) {
            log.warn("비밀번호 미입력");
            throw new BusinessException(ErrorCode.CAPSULE_PASSWORD_REQUIRED);
        }

        // 로그인 상태 확인
        boolean isLoggedIn = isUserLoggedIn();
        Long memberId = isLoggedIn ? currentUserContext.getCurrentMemberId() : null;
        String viewerType = isLoggedIn ? "MEMBER" : "GUEST";

        if (!phoneCrypto.verifyHash(requestDto.password(), capsule.getCapPassword())) {
            log.warn("비밀번호 불일치");

            // 로그 생성 및 저장
            CapsuleOpenLog passwordFailLog  = createOpenLog(
                    capsule,
                    requestDto,
                    CapsuleOpenStatus.FAIL_PASSWORD,
                    memberId,
                    viewerType
            );
            capsuleOpenLogService.saveLogInNewTransaction(passwordFailLog );

            throw new BusinessException(ErrorCode.CAPSULE_PASSWORD_NOT_MATCH);
        }

        boolean hasAlreadyViewed = hasAlreadyOpenedUnprotected(capsule.getCapsuleId(), memberId, requestDto.ipAddress());

        if (hasAlreadyViewed) {
            return handleUnprotectedReview(capsule, requestDto, memberId, viewerType);
        }

        // 조건 검증 + 이상 감지
        UnlockValidationResult validationResult = unlockService.validateUnlockConditionsForPrivate(
                capsule,
                requestDto.serverTime(),
                requestDto.locationLat(),
                requestDto.locationLng(),
                requestDto.unlockAt(),
                memberId,
                requestDto.ipAddress()
        );

        // 로그 생성
        CapsuleOpenStatus status = determineOpenStatus(validationResult, capsule.getUnlockType());

        // 실패 시 이상 탐지 및 제재 처리
        if (!validationResult.isSuccess()) {
            CapsuleOpenLog failLog = createOpenLog(
                    capsule,
                    requestDto,
                    status,
                    memberId,
                    viewerType
            );

            detectAndHandleAnomaly(failLog, validationResult, memberId, requestDto.ipAddress());

            capsuleOpenLogRepository.save(failLog);

            if (validationResult.hasAnomaly()) {
                throwAnomalyException(validationResult, memberId);
            }

            log.warn("조건 미충족 또는 이상 활동 감지");
            return CapsuleConditionResponseDTO.failFrom(capsule);
        }

        CapsuleOpenLog successLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                memberId,
                viewerType
        );
        capsuleOpenLogRepository.save(successLog);

        detectAndHandleAnomaly(successLog, validationResult, memberId, requestDto.ipAddress());
        if (validationResult.hasAnomaly()) {
            throwAnomalyException(validationResult, memberId);
        }


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

        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                requestDto,
                CapsuleOpenStatus.SUCCESS,
                memberId,
                viewerType
        );
        capsuleOpenLogRepository.save(openLog);


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
                    .existsByCapsuleId_CapsuleIdAndMemberIdAndStatus(
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

            incrementViewCountViaRedis(capsule.getCapsuleId());
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

        Long currentMemberId = currentUserContext.getCurrentMemberId();

        // 첫 조회일 때만 조회수 증가
        if (shouldIncrement) {
            incrementViewCountViaRedis(capsule.getCapsuleId());
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

        // 처음 조회하면 조회수 증가
        if (shouldIncrement) {
            incrementViewCountViaRedis(capsule.getCapsuleId());
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
        return CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(memberId)
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

    // Redis 이용 조회수 증가
    private void incrementViewCountViaRedis(Long capsuleId) {
        String key = VIEW_COUNT_KEY_PREFIX + capsuleId;

        try {
            Long newCount = redisTemplate.opsForValue().increment(key);

            if (newCount != null && newCount == 1) {
                redisTemplate.expire(key, VIEW_COUNT_TTL);
                log.debug("Redis 조회수 TTL 설정 - capsuleId: {}, TTL: {}분",
                        capsuleId, VIEW_COUNT_TTL.toMinutes());
            }
        } catch (Exception e) {
            log.error("Redis 장애 발생 - DB 직접 업데이트로 폴백. capsuleId: {}", capsuleId, e);

            try {
                capsuleRepository.incrementViewCount(capsuleId);
                log.info("DB 직접 업데이트 성공 - capsuleId: {}", capsuleId);
            } catch (Exception dbError) {
                log.error("DB 업데이트도 실패 - capsuleId: {}", capsuleId, dbError);
            }
        }
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
        List<CapsuleAttachment> list = capsuleAttachmentRepository
                .findAllByCapsule_CapsuleIdAndStatus(capsuleId, CapsuleAttachmentStatus.USED);

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<CapsuleAttachmentViewResponse>> futures =  list.stream()
                .map(attachment -> CompletableFuture.supplyAsync(
                        () -> getPresignedUrlWithCache(attachment),
                        s3ExecutorService
                ))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private CapsuleAttachmentViewResponse getPresignedUrlWithCache(
            CapsuleAttachment attachment
    ) {
        String cacheKey = PRESIGNED_URL_KEY_PREFIX + attachment.getS3Key();

        try {
            // 1. 캐시 조회
            String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

            if (cachedUrl != null) {
                log.debug("Presigned URL 캐시 히트 - S3Key: {}", attachment.getS3Key());
                return new CapsuleAttachmentViewResponse(cachedUrl, attachment.getId());
            }

            // 2. 캐시 미스 - S3 API 호출
            log.debug("Presigned URL 캐시 미스 - S3 API 호출 - S3Key: {}",
                    attachment.getS3Key());

            String presignedUrl = presignedUrlProvider.presignGet(
                    attachment.getS3Key(),
                    PRESIGNED_URL_VALIDITY
            );

            // 3. 캐시 저장
            redisTemplate.opsForValue().set(
                    cacheKey,
                    presignedUrl,
                    PRESIGNED_URL_TTL
            );

            return new CapsuleAttachmentViewResponse(presignedUrl, attachment.getId());

        } catch (Exception e) {
            log.error("Redis 캐시 오류 - 폴백: S3 직접 호출 - S3Key: {}",
                    attachment.getS3Key(), e);

            String presignedUrl = presignedUrlProvider.presignGet(
                    attachment.getS3Key(),
                    PRESIGNED_URL_VALIDITY
            );

            return new CapsuleAttachmentViewResponse(presignedUrl, attachment.getId());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("S3 ExecutorService 종료 시작");
        s3ExecutorService.shutdown();

        try {
            if (!s3ExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("S3 ExecutorService 정상 종료 실패 - 강제 종료");
                s3ExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("S3 ExecutorService 종료 중 인터럽트 발생", e);
            s3ExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // 스토리트랙 캡슐 조회 시, 캡슐 재조회
    @Transactional
    public CapsuleConditionResponseDTO readAlreadyOpendeStorytrackCapsule(
            Capsule capsule,
            CapsuleConditionRequestDTO request,
            Long memberId
    ){
        if(!"PUBLIC".equals(capsule.getVisibility())){
            throw new BusinessException(ErrorCode.NOT_PUBLIC);
        }

        // 재조회 로그 기록
        CapsuleOpenLog openLog = createOpenLog(
                capsule,
                request,
                CapsuleOpenStatus.SUCCESS,
                memberId,
                "MEMBER"
        );
        capsuleOpenLogService.saveLogInNewTransaction(openLog);

        // 조건 검증 없이 바로 읽기
        return readPublicCapsule(
                capsule,
                request,
                true // 재조회 플래그
        );

    }
}
