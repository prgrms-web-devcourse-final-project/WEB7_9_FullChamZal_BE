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

    public Capsule getCapsule(Long capsuleId) {
        Optional<Capsule> resultCapsule = capsuleRepository.findById(capsuleId);
        if (resultCapsule.isPresent()) {

            //내역 남기기

            return resultCapsule.get();
        }else{
            return null;
        }
    }


    public boolean senderVerification(Capsule capsule, String phoneNumber, LocalDateTime unlockAt, Double locationLat, Double locationLng) {
        String hashedPhoneNumber = phoneCrypto.hash(phoneNumber);

        CapsuleRecipient capsuleRecipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        if(capsuleRecipient.getRecipientPhoneHash().equals(hashedPhoneNumber)){
            //두 값이 같다면
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
        }else{
            //같지 않다면 403 에러
            throw new BusinessException(ErrorCode.CAPSULE_NOT_RECEIVER);
        }

    }
}
