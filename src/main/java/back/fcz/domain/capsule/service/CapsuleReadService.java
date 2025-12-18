package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleReadRequest;
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

    //조건확인하고 검증됐다면 읽기
    @Transactional
    public CapsuleConditionResponseDTO conditionAndRead(CapsuleConditionRequestDTO requestDto) {
        Capsule capsule = capsuleRepository.findById(requestDto.capsuleId()).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        //자신에게 보내는 캡슐인 경우(시공간 검증만)
        if(capsule.getVisibility().equals("SELF")){
            // 시간/위치 조건 검증
            boolean conditionMet = unlockService.validateUnlockConditionsForPrivate(
                    capsule,
                    requestDto.unlockAt(),
                    requestDto.locationLat(),
                    requestDto.locationLng()
            );

            if(conditionMet){
                return readMemberCapsule(capsule, requestDto);
            } else {
                throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
            }
        }

        // 1. 공개인지 비공개인지
        if(capsule.getVisibility().equals("PUBLIC")){
            //공개 캡슐로직
            log.info("공개 캡슐 로직 진입 - capsuleId: {}", capsule.getCapsuleId());
            return publicCapsuleLogic(capsule, requestDto);
        }else{
            //개인 캡슐로직
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
    public CapsuleConditionResponseDTO privateCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        //전화번호 기반인지 url+비번 기반인지를 먼저 확인하고 조회 횟수를 검증할것


        //2. 전화번호 기반인지 url+비번 기반인지
        if( !(requestDto.password() == null || requestDto.password().isBlank()) ){
            System.out.println("url+비밀번호 기반 캡슐");
            //url+비번 기반 -> 수신자가 회원인지 비회원인지 판단
            if(capsuleRecipientRepository.existsByCapsuleId_CapsuleId(capsule.getCapsuleId())){
                //수신자 회원
                System.out.println("url+비밀번호 기반 캡슐 : 수신자 회원");
                //이제 기존에 조회 했던 것인지 검증
                if(capsule.getCurrentViewCount() > 0){
                    //기존에 조회함 -> 바로 조회가능
                    return readMemberCapsule(capsule, requestDto);
                }else{
                    //처음 조회하는 것 -> 검증 로직 통과하면 조회가능
                    if(urlAndPasswordVerification(
                            capsule, requestDto.password(), capsule.getCapPassword(), requestDto.unlockAt(), requestDto.locationLat(), requestDto.locationLng())
                    ){
                        return readMemberCapsule(capsule, requestDto);
                    }else{
                        throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
                    }
                }
            }else{
                //수신자 비회원
                System.out.println("url+비밀번호 기반 캡슐 : 수신자 비회원");
                if(capsule.getCurrentViewCount() > 0){
                    //기존에 조회함 -> 바로 조회가능
                    return readNonMemberCapsule(capsule, requestDto);
                }else{
                    //처음 조회하는 것 -> 검증 로직 통과하면 조회가능
                    if(urlAndPasswordVerification(
                            capsule, requestDto.password(), capsule.getCapPassword(), requestDto.unlockAt(), requestDto.locationLat(), requestDto.locationLng()
                    )){
                        return readNonMemberCapsule(capsule, requestDto);
                    }else{
                        throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
                    }
                }
            }
        }else{
            //전화번호 기반 -> 수신자는 회원
            System.out.println("전화번호 기반 캡슐");
            if(capsule.getCurrentViewCount()>0){
                //기존에 조회함 -> 바로 조회가능
                return readMemberCapsule(capsule, requestDto);
            }else{
                InServerMemberResponse user = currentUserContext.getCurrentUser();
                MemberDetailResponse response = memberService.getDetailMe(user);
                String phoneNumber = response.phoneNumber();
                System.out.println("response.phoneNumber() : "+response.phoneNumber());

                if(phoneNumberVerification(
                        capsule, phoneNumber,  requestDto.unlockAt(), requestDto.locationLat(), requestDto.locationLng()
                )){
                    return readMemberCapsule(capsule, requestDto);
                }else{
                    throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
                }
            }
        }

    }

    // 전화번호 검증 로직
    public boolean phoneNumberVerification(
            Capsule capsule, String phoneNumber, LocalDateTime unlockAt, Double locationLat, Double locationLng
    ) {
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

    //url+비밀번호 검증 로직
    public boolean urlAndPasswordVerification(
            Capsule capsule, String password, String capsulePassword, LocalDateTime unlockAt, Double locationLat, Double locationLng
    ) {
        //비밀번호 검증
        System.out.println("password : " + password);
        System.out.println("capsulePassword : " + capsulePassword);
        if(!phoneCrypto.verifyHash(password, capsulePassword)){
            throw new BusinessException(ErrorCode.CAPSULE_PASSWORD_NOT_MATCH);
        }

        //캡슐 해제 조건 검증
        return unlockService.validateUnlockConditionsForPrivate(
                capsule, unlockAt, locationLat, locationLng
        );
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

    //개인 캡슐 읽기 - 수신자가 회원인 경우(로그 + CapsuleRecipient를 남김)
    public CapsuleConditionResponseDTO readMemberCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto){
        String viewerType = currentUserContext.getCurrentUser().role().getDescription();
        Long currentMemberId = currentUserContext.getCurrentMemberId();
        Member member = memberRepository.findById(currentMemberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(member)
                .viewerType(viewerType)
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(null)
                .ipAddress(null)
                .build();
        capsuleOpenLogRepository.save(log);
        capsule.increasedViewCount();

        CapsuleRecipient recipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPIENT_NOT_FOUND));

        recipient.setUnlockedAt(requestDto.unlockAt());
        capsuleRecipientRepository.save(recipient);

        return CapsuleConditionResponseDTO.from(capsule, recipient);
    }
    //개인 캡슐 읽기 - 수신자가 비회원인 경우(로그만 남김)
    public CapsuleConditionResponseDTO readNonMemberCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto){
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
        capsule.increasedViewCount();
        //수신자가 비회원인 경우는 CapsuleRecipient 자체가 없으니 갱신도 불가능

        return CapsuleConditionResponseDTO.from(capsule);
    }

    // 사용자 로그인 여부
    private boolean isUserLoggedIn() {
        try {
            currentUserContext.getCurrentMemberId();
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    public CapsuleReadResponse existedPassword(CapsuleReadRequest request){
        Capsule capsule = capsuleRepository.findById(request.capsuleId()).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        if(capsule.getCapPassword()==null){
            return CapsuleReadResponse.from(false);
        }else{
            return CapsuleReadResponse.from(true);
        }
    }
}
