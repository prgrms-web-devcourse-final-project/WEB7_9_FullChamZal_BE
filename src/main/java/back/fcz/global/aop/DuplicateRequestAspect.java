package back.fcz.global.aop;

import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DuplicateRequestAspect {
    private final CurrentUserContext currentUserContext;

    private final Map<String, Long> requestMap = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 2000; // 2초

    @Pointcut("within(*..*Controller)")
    public void onRequest() {}

    public Optional<InServerMemberResponse> tryGetCurrentUser() {
        try {
            return Optional.ofNullable(currentUserContext.getCurrentUser());
        } catch (BusinessException e) {
            return Optional.empty();
        }
    }

    @Around("onRequest()")
    public Object duplicateRequestCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (method.isAnnotationPresent(AllowDuplicateRequest.class)) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String httpMethod = request.getMethod();

        // GET 메소드인 경우 중복 체크를 하지 않음
        if ("GET".equalsIgnoreCase(httpMethod)) {
            return joinPoint.proceed();
        }

        // 회원 여부에 따라 memebrId / null로 값이 다름
        Optional<InServerMemberResponse> optionalUser = tryGetCurrentUser();
        String requestId;

        if(optionalUser.isPresent()){ // 로그인 상태
            Long memberId = optionalUser.get().memberId(); // memberId를 식별자로 사용

            requestId = memberId + ":" +
                    request.getMethod() + ":" +
                    request.getRequestURI();
        }else{ // 비로그인 상태일 때
            String clientKey = request.getRemoteAddr() + ":" +
                    request.getHeader("User-Agent"); // 요청을 보낸 HTTP IP / user Agent 를 식별자로 사용
            requestId =
                    clientKey + ":" +
                            request.getMethod() + ":" +
                            request.getRequestURI();
        }

        long now = System.currentTimeMillis();

        // requestId(요청을 구분하기 위한 식별자)를 선점 시도
        Long prev = requestMap.putIfAbsent(requestId, now);

        // 같은 requestId 요청이 쿨타임 내에 다시 들어 왔을 때 -> 중복된 요청임을 표시해야함
        if (prev != null && now - prev < COOLDOWN_MS) {
            return handleDuplicateRequest();
        }

        // 같은 requstId가 새로 들어 왔을 때, 가장 마지막 요청으로부터 쿨다운 시간만큼 지났는지 확인
        if (prev != null && now - prev >= COOLDOWN_MS) {
            requestMap.put(requestId, now);
        }

        // 핵심 로직 실행
        return joinPoint.proceed();
    }

    private ResponseEntity<Object> handleDuplicateRequest() {
        // 중복 요청에 대한 응답 처리
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body("중복된 요청 입니다.");
    }
}
