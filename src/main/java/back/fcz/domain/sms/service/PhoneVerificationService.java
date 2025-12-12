package back.fcz.domain.sms.service;

import back.fcz.domain.sms.dto.request.ConfirmSmsCodeRequest;
import back.fcz.domain.sms.dto.request.SendSmsCodeRequest;
import back.fcz.domain.sms.dto.response.ConfirmSmsCodeResponse;
import back.fcz.domain.sms.dto.response.SendSmsCodeResponse;
import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.infra.sms.CoolSmsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final int CODE_LENGTH = 6;   // 인증 코드 길이
    private static final int CODE_EXPIRATION_MINUTES = 3;   // 인증 코드 만료 시간
    private static final int MAX_ATTEMPTS = 5;  // 최대 시도 횟수
    private static final int RESEND_COOLDOWN_SECONDS = 30;  // 재전송 쿨다운 시간

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final CoolSmsClient coolSmsClient;
    private final PhoneCrypto phoneCrypto;

    // 인증 코드 발송 로직
    @Transactional
    public SendSmsCodeResponse sendCode(
            SendSmsCodeRequest request
    ) {
        String nomalizedPhoneNumber = nomalizePhoneNumber(request.phoneNumber());
        String phoneNumberHash = phoneCrypto.hash(nomalizedPhoneNumber);
        PhoneVerificationPurpose purpose = request.purpose();
        LocalDateTime now = LocalDateTime.now();

        // 쿨다운 : 이전 코드 발송 후 일정 시간 이내 재발송 불가
        LocalDateTime cooldownThreshold = now.minusSeconds(RESEND_COOLDOWN_SECONDS);
        long recentCount = phoneVerificationRepository.countByPhoneNumberHashAndPurposeAndCreatedAtAfter(
                phoneNumberHash,
                purpose,
                cooldownThreshold
        );
        if(recentCount > 0){
            throw new BusinessException(ErrorCode.SMS_RESEND_COOLDOWN);
        }

        // 최신 보류중인 인증 건 조회
        PhoneVerification latestPending = phoneVerificationRepository
                .findLatestPending(phoneNumberHash, purpose)
                .orElse(null);
        // 코드 생성 및 해싱
        String code = generateCode(CODE_LENGTH);
        String codeHash = phoneCrypto.hash(code);

        PhoneVerification verification;

        if(!request.resend()){
            // 신규 인증 건 생성
            verification = new PhoneVerification(
                    phoneNumberHash,
                    codeHash,
                    purpose
            );
            phoneVerificationRepository.save(verification);
        } else {
            if(latestPending == null || latestPending.isExpired(now)){
                if(latestPending != null && latestPending.isExpired(now)){
                    latestPending.markExpired();
                }
                // 재전송이지만 기존 보류중인 인증 건이 없거나 만료된 경우, 신규 인증 건 생성
                verification = new PhoneVerification(
                        phoneNumberHash,
                        codeHash,
                        purpose
                );
                phoneVerificationRepository.save(verification);
            } else {
                // 기존 보류중인 인증 건이 유효한 경우, 코드만 갱신
                latestPending.reset(codeHash,now);
            }
        }

        // SMS 발송
        String prefix = request.resend() ? "[재전송]" : "";
        String message = prefix + "[Dear._] 인증번호 [" + code + "]를 입력해주세요. (유효시간 " + CODE_EXPIRATION_MINUTES + "분)";
        coolSmsClient.sendSms(nomalizedPhoneNumber, message);

        return new SendSmsCodeResponse(true, RESEND_COOLDOWN_SECONDS);
    }

    // 인증 코드 검증 로직
    @Transactional
    public ConfirmSmsCodeResponse confirmCode(ConfirmSmsCodeRequest request){
        String phoneNumberHash = phoneCrypto.hash(nomalizePhoneNumber(request.phoneNumber()));
        PhoneVerificationPurpose purpose = request.purpose();
        String codeHash = phoneCrypto.hash(request.verificationCode());

        LocalDateTime now = LocalDateTime.now();
        PhoneVerification verification = phoneVerificationRepository
                .findLatestPending(phoneNumberHash, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        // 만료 체크
        if (verification.isExpired(now)){
            verification.markExpired();
            throw new BusinessException(ErrorCode.VERIFICATION_EXPIRED);
        }
        // 시도 횟수 초과 체크
        if(verification.getAttemptCount() >= MAX_ATTEMPTS){
            verification.markExpired();
            throw new BusinessException(ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);
        }
        // 코드 검증
        boolean matched = phoneCrypto.verifyHash(codeHash, verification.getCode());
        if(!matched) {
            verification.incrementAttemptCount();
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }
        // 검증 성공 처리
        verification.markVerified(now);

        return new ConfirmSmsCodeResponse(true);
    }


    // ------- 헬퍼 메서드 ------- //

    // 인증 확인 여부
    public boolean isPhoneVerified(String PhoneNumber, PhoneVerificationPurpose purpose){
        String nomalizedPhoneNumber = nomalizePhoneNumber(PhoneNumber);
        String phoneNumberHash = phoneCrypto.hash(nomalizedPhoneNumber);

        return phoneVerificationRepository
                .findTop1ByPhoneNumberHashAndPurposeOrderByCreatedAtDesc(phoneNumberHash, purpose)
                .filter(pv -> pv.getStatus() == PhoneVerificationStatus.VERIFIED)
                .isPresent();
    }

    // 전화번호 정규화
    private String nomalizePhoneNumber(String phoneNumber){
        if(phoneNumber == null) return null;
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    // 인증 코드 생성
    private String generateCode(int length){
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<length; i++){
            int digit = random.nextInt(10); // 0~9 사이의 랜덤 숫자 생성
            sb.append(digit);
        }
        return sb.toString();
    }
}
