package back.fcz.infra.sms;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsSender {

    private final CoolSmsClient coolSmsClient;

    @RateLimiter(name = "coolsmsSend")
    @Retry(name = "coolsmsSend")
    @CircuitBreaker(name = "coolsmsSend", fallbackMethod = "fallbackSend")
    public void send(String phoneNumber, String message) {
        coolSmsClient.sendSms(phoneNumber, message);
    }

    // CircuitBreaker OPEN or 연쇄 장애 시 여기로 떨어짐
    private void fallbackSend(String phoneNumber, String message, Throwable t) {

        throw new BusinessException(ErrorCode.SMS_PROVIDER_UNAVAILABLE); //  수정
    }
}
