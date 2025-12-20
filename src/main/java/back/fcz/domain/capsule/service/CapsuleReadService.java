package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleReadResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
            System.out.println("개인 캡슐 로직");
            return privateCapsuleLogic(capsule, requestDto);
        }
    }

    //공개 캡슐
    @Transactional
    public CapsuleConditionResponseDTO publicCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        log.info("=== 공개 캡슐 로직 시작 - capsuleId: {} ===", capsule.getCapsuleId());

        // 공개 캡슐은 회원만 조회 가능 - 로그인 체크
        if (!isUserLoggedIn()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        Long currentMemberId = currentUserContext.getCurrentMemberId();
        log.info("로그인 회원 - memberId: {}", currentMemberId);

        log.info("첫 조회 - 검증 시작");

        // 시간/위치 조건 검증
        boolean conditionMet = unlockService.validateTimeAndLocationConditions(
                capsule, requestDto.unlockAt(), requestDto.locationLat(), requestDto.locationLng()
        );

        if (!conditionMet) {
            log.warn("시간/위치 조건 미충족");
            throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
        }

        log.info("시간/위치 조건 통과");

        if (firstComeService.hasFirstComeLimit(capsule)) {
            log.info("선착순 제한 있음 - maxViewCount: {}", capsule.getMaxViewCount());

            // 선착순 검증과 PublicCapsuleRecipient 저장을 원자적으로 처리
            boolean isNewView = firstComeService.tryIncrementViewCountAndSaveRecipient(
                    capsule.getCapsuleId(),
                    currentMemberId,
                    requestDto.unlockAt()
            );

            log.info("선착순 검증 및 저장 완료");
            return readPublicCapsule(capsule, requestDto, !isNewView);
        } else {
            log.info("선착순 없음 - 바로 저장");

            boolean isFirstTimeViewing = !publicCapsuleRecipientRepository
                    .existsByCapsuleId_CapsuleIdAndMemberId(capsule.getCapsuleId(), currentMemberId);

            if (isFirstTimeViewing) {
                PublicCapsuleRecipient publicCapsuleLog = PublicCapsuleRecipient.builder()
                        .capsuleId(capsule)
                        .memberId(currentMemberId)
                        .unlockedAt(requestDto.unlockAt())
                        .build();
                publicCapsuleRecipientRepository.save(publicCapsuleLog);
                log.info("저장 완료");
            } else {
                log.info("재조회 - 저장 건너뜀");
            }

            log.info("=== 공개 캡슐 로직 종료 ===");
            return readPublicCapsule(capsule, requestDto, !isFirstTimeViewing);
        }
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
            throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
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
            throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
        }

        log.info("시간/위치 조건 통과 - 캡슐 읽기 허용, 로그인 여부: {}", isLoggedIn);

        if (isLoggedIn) {
            return readMemberCapsuleWithoutRecipient(capsule, requestDto, true);
        } else {
            return readCapsuleAsGuest(capsule, requestDto, true);
        }
    }

    // 전화번호 검증 로직
    public boolean phoneNumberVerification(Capsule capsule, String phoneNumber, LocalDateTime unlockAt, Double locationLat, Double locationLng) {
        System.out.println("전화번호 검증 시작");
        CapsuleRecipient capsuleRecipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPIENT_NOT_FOUND));

        if(phoneCrypto.verifyHash(phoneNumber, capsuleRecipient.getRecipientPhoneHash())){
            //두 값이 같다면 해제 조건 확인
            return unlockService.validateUnlockConditionsForPrivate(
                    capsule, unlockAt, locationLat, locationLng
            );
        }else{
            //같지 않다면 403 에러
            throw new BusinessException(ErrorCode.CAPSULE_NOT_RECEIVER);
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

        // viewStatus = false: 처음 조회
        // viewStatus = true: 재조회
        boolean isFirstView = !viewStatus;
        boolean hasFirstCome = firstComeService.hasFirstComeLimit(capsule);

        // 조회수 증가 조건:
        // 1. 처음 조회이고
        // 2. 선착순이 없는 경우만
        // (선착순 있으면 FirstComeService에서 이미 증가됨)
        if (isFirstView && !hasFirstCome) {
            capsule.increasedViewCount();
        }

        return CapsuleConditionResponseDTO.from(capsule, viewStatus);

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

        return CapsuleConditionResponseDTO.from(capsule, recipient);
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

        return CapsuleConditionResponseDTO.from(capsule);
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

        return CapsuleConditionResponseDTO.from(capsule);
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
}
