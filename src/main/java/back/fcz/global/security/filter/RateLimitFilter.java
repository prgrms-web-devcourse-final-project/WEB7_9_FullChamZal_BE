package back.fcz.global.security.filter;

import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.sanction.constant.RiskLevel;
import back.fcz.domain.sanction.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String[] EXCLUDED_PATHS = {
            "/h2-console",
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        for (String excludedPath : EXCLUDED_PATHS) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();

        if (currentUserContext.isAuthenticated()) {
            Long memberId = currentUserContext.getCurrentMemberId();

            // 기본(LOW) 레벨 rate-limit 적용
            rateLimitService.apply(memberId, RiskLevel.LOW);

        } else {
            // 비회원(IP) 기준 기본 rate-limit 적용
            rateLimitService.applyByIp(clientIp, RiskLevel.LOW);
        }

        filterChain.doFilter(request, response);
    }
}