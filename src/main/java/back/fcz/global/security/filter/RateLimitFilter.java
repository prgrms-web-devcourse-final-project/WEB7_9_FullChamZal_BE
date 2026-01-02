package back.fcz.global.security.filter;

import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.sanction.constant.RiskLevel;
import back.fcz.domain.sanction.service.RateLimitService;
import back.fcz.domain.sanction.util.RequestInfoExtractor;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limit 검증 필터
 * 쿨다운 상태의 사용자 요청을 제한합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final CurrentUserContext currentUserContext;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return !path.equals("/api/v1/capsule/read");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = RequestInfoExtractor.extractIp(request);

        if (currentUserContext.isAuthenticated()) {
            Long memberId = currentUserContext.getCurrentMemberId();

            // 쿨다운 체크
            if (rateLimitService.isInCooldown(memberId)) {
                sendRateLimitResponse(response);
                return;
            }

            // 기본(LOW) 레벨 rate-limit 적용
            rateLimitService.apply(memberId, RiskLevel.LOW);

        } else {
            // 쿨다운 체크
            if (rateLimitService.isInCooldownByIp(clientIp)) {
                sendRateLimitResponse(response);
                return;
            }

            // 비회원(IP) 기준 기본 rate-limit 적용
            rateLimitService.applyByIp(clientIp, RiskLevel.LOW);
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(errorCode);
        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }
}