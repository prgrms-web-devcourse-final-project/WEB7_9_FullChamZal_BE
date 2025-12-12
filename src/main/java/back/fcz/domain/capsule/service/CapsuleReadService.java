package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.unlock.service.UnlockService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CapsuleReadService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final PhoneCrypto phoneCrypto;
    private final UnlockService unlockService;
    private final PasswordEncoder passwordEncoder;

    public Capsule getCapsule(Long capsuleId) {
        Optional<Capsule> resultCapsule = capsuleRepository.findById(capsuleId);
        if (resultCapsule.isPresent()) {
            return resultCapsule.get();
        }else{
            throw new BusinessException(ErrorCode.CAPSULE_NOT_FOUND);
        }
    }

    private boolean capsuleCondition(Capsule capsule, LocalDateTime unlockAt, Double locationLat, Double locationLng) {
        if(capsule.getUnlockType().equals("TIME") && unlockService.isTimeConditionMet(capsule.getCapsuleId(), unlockAt)) {
            return true;
        }else if(capsule.getUnlockType().equals("LOCATION") && unlockService.isLocationConditionMet(capsule.getCapsuleId(), locationLat, locationLng)) {
            return true;
        }else if (capsule.getUnlockType().equals("TIME_AND_LOCATION") && unlockService.isTimeAndLocationConditionMet(capsule.getCapsuleId(), unlockAt, locationLat, locationLng)) {
            return true;
        }else{
            //   시간/위치 검증 실패
            throw new BusinessException(ErrorCode.NOT_OPENED_CAPSULE);
        }
    }


    public boolean phoneNumberVerification(
            Capsule capsule, String phoneNumber, LocalDateTime unlockAt, Double locationLat, Double locationLng
    ) {
        CapsuleRecipient capsuleRecipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        if(phoneCrypto.verifyHash(phoneNumber, capsuleRecipient.getRecipientPhoneHash())){
            //두 값이 같다면
            return capsuleCondition(capsule, unlockAt, locationLat, locationLng);
        }else{
            //같지 않다면 403 에러
            throw new BusinessException(ErrorCode.CAPSULE_NOT_RECEIVER);
        }

    }

    public boolean urlAndPasswordVerification(
            Capsule capsule, String password, String capsulePassword, LocalDateTime unlockAt, Double locationLat, Double locationLng
    ) {
        //1. 비밀번호 검증
        if(!passwordEncoder.matches(password, capsulePassword)){
            throw new BusinessException(ErrorCode.CAPSULE_PASSWORD_NOT_MATCH);
        }

        //2. 해제 조건 검증
        return capsuleCondition(capsule, unlockAt, locationLat, locationLng);


        //3. 저장 여부 선택


        //4. 저장 여부 선택에 따라 회원가입/로그인 권유


        //5. 캡슐 저장 처리


    }
}
