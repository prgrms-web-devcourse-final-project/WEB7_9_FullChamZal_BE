package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
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
import back.fcz.domain.unlock.service.UnlockService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleReadService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final PhoneCrypto phoneCrypto;
    private final UnlockService unlockService;
    private final MemberRepository memberRepository;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final MemberService memberService;
    private final CurrentUserContext currentUserContext;

    //조건확인하고 검증됐다면 읽기
    public CapsuleConditionResponseDTO conditionAndRead(CapsuleConditionRequestDTO requestDto) {
        Capsule capsule = capsuleRepository.findById(requestDto.capsuleId()).orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        //자신에게 보내는 캡슐인 경우(시공간 검증만)
        if(requestDto.isSendSelf()==1){
            if(capsuleCondition(capsule, requestDto.unlockAt(), requestDto.locationLat(), requestDto.locationLng())){
                return readMemberCapsule(capsule, requestDto);
            }
        }

        // 1. 공개인지 비공개인지
        if(capsule.getVisibility().equals("PUBLIC")){
            //공개 캡슐로직
            System.out.println("공개 캡슐 로직");
            return publicCapsuleLogic(capsule, requestDto);
        }else{
            //개인 캡슐로직
            System.out.println("개인 캡슐 로직");
            return privateCapsuleLogic(capsule, requestDto);
        }

    }

    //공개 캡슐
    public CapsuleConditionResponseDTO publicCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        Long currentMemberId = currentUserContext.getCurrentMemberId();
        //2. 조회 횟수 검증
        if(publicCapsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndMemberId(capsule.getCapsuleId(), currentMemberId)){
            System.out.println("기존에 조회 된 공개 캡슐");
            //기존에 조회했던 것이니 바로 조회가능
            boolean viewStatus = true;
            return readPublicCapsule(capsule, requestDto, viewStatus);
        }else{
            System.out.println("기존에 조회 되지 않은 공개 캡슐");
            boolean viewStatus = false;

            //조회한 적 없으니 조건 검증이 필요 / 공개 캡슐은 시간, 위치만 검증하면됨
            if(capsuleCondition(capsule, requestDto.unlockAt(), requestDto.locationLat(), requestDto.locationLng())){
                PublicCapsuleRecipient publicCapsuleLog = PublicCapsuleRecipient.builder()
                        .capsuleId(capsule)
                        .memberId(currentMemberId)
                        .unlockedAt(requestDto.unlockAt())
                        .build();
                publicCapsuleRecipientRepository.save(publicCapsuleLog);
                System.out.println("publicCapsuleLog 저장 완료");
                return readPublicCapsule(capsule, requestDto, viewStatus);
            }else{// 검증 실패
                throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
            }
        }
    }

    //개인 캡슐
    public CapsuleConditionResponseDTO privateCapsuleLogic(Capsule capsule, CapsuleConditionRequestDTO requestDto) {
        //전화번호 기반인지 url+비번 기반인지를 먼저 확인하고 조회 횟수를 검증할것
        

        //2. 전화번호 기반인지 url+비번 기반인지
        if( !(requestDto.url() == null || requestDto.url().isBlank()) ){
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
            return capsuleCondition(capsule, unlockAt, locationLat, locationLng);
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
        return capsuleCondition(capsule, unlockAt, locationLat, locationLng);
    }

    //캡슐 해제 조건 검증
    private boolean capsuleCondition(Capsule capsule, LocalDateTime unlockAt, Double locationLat, Double locationLng) {
        System.out.println("검증 로직 진입");
        if(capsule.getUnlockType().equals("TIME") && unlockService.isTimeConditionMet(capsule.getCapsuleId(), unlockAt)) {
            System.out.println("시간 검증 통과");
            return true;
        }else if(capsule.getUnlockType().equals("LOCATION") && unlockService.isLocationConditionMet(capsule.getCapsuleId(), locationLat, locationLng)) {
            System.out.println("공간 검증 통과");
            return true;
        }else if (capsule.getUnlockType().equals("TIME_AND_LOCATION") && unlockService.isTimeAndLocationConditionMet(capsule.getCapsuleId(), unlockAt, locationLat, locationLng)) {
            System.out.println("시공간 검증 통과");
            return true;
        }else{
            //   시간/위치 검증 실패
            System.out.println("unlockAt : " + unlockAt);
            System.out.println("capsuleUnlockAt : " +  capsule.getUnlockAt());
            System.out.println("locationLat : " + locationLat);
            System.out.println("capsuleLocationLat" +  capsule.getLocationLat());
            System.out.println("locationLng : " + locationLng);
            System.out.println("capsuleLocationLat" +  capsule.getLocationLng());
            throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
        }
    }

    //공개 캡슐 읽기
    public CapsuleConditionResponseDTO readPublicCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto, boolean viewStatus) {

        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(null)
                .viewerType(requestDto.viewerType())
                .openedAt(requestDto.unlockAt())
                .currentLat(requestDto.locationLat())
                .currentLng(requestDto.locationLng())
                .userAgent(null)
                .ipAddress(null)
                .build();
        capsuleOpenLogRepository.save(log);
        capsule.increasedViewCount();
        return CapsuleConditionResponseDTO.from(capsule, viewStatus);

    }

    //개인 캡슐 읽기 - 수신자가 회원인 경우(로그 + CapsuleRecipient를 남김)
    public CapsuleConditionResponseDTO readMemberCapsule(Capsule capsule, CapsuleConditionRequestDTO requestDto){
        Long currentMemberId = currentUserContext.getCurrentMemberId();
        Member member = memberRepository.findById(currentMemberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(member)
                .viewerType(requestDto.viewerType())
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
        CapsuleOpenLog log = CapsuleOpenLog.builder()
                .capsuleId(capsule)
                .memberId(null)
                .viewerType(requestDto.viewerType())
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

}
