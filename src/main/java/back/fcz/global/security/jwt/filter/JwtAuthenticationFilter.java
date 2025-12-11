package back.fcz.global.security.jwt.filter;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.UserType;
import back.fcz.global.security.jwt.service.TokenBlacklistService;
import back.fcz.global.security.jwt.util.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Optional<String> tokenOpt = CookieUtil.getCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE);


            if (tokenOpt.isEmpty()) {
                log.debug("Access Token이 없습니다. URI: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken = tokenOpt.get();
            log.debug("Access Token 추출 완료 - URI: {}", request.getRequestURI());

            if (tokenBlacklistService.isBlacklisted(accessToken)) {
                log.warn("블랙리스트에 등록된 토큰 사용 시도. URI: {}", request.getRequestURI());
                throw new BusinessException(ErrorCode.TOKEN_BLACKLISTED);
            }

            Long memberId = jwtProvider.extractMemberId(accessToken);
            String role = jwtProvider.extractRole(accessToken);
            UserType userType = jwtProvider.extractUserType(accessToken);

            log.debug("JWT 인증 성공 - memberId: {}, role: {}, userType: {}", memberId, role, userType);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    memberId,
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("SecurityContext에 인증 정보 설정 완료 - memberId: {}", memberId);

            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            log.error("JWT 인증 실패 - ErrorCode: {}, Message: {}, URI: {}",
                    e.getErrorCode(), e.getMessage(), request.getRequestURI());
            handleAuthenticationException(response, e);
            return;
        } catch (Exception e) {
            log.error("JWT 필터에서 예상치 못한 오류 발생 - URI: {}, Error: {}",
                    request.getRequestURI(), e.getMessage(), e);
            handleUnexpectedException(response, e);
            return;
        }
    }

    // 400 에러 - 비즈니스 예외
    private void handleAuthenticationException(HttpServletResponse response, BusinessException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", e.getErrorCode().name());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        log.info("인증 예외 응답 전송 완료 - ErrorCode: {}", e.getErrorCode());
    }

    // 500 에러 - 서버 오류
    private void handleUnexpectedException(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", "INTERNAL_ERROR");
        errorResponse.put("message", "인증 처리 중 오류가 발생했습니다.");
        errorResponse.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        log.error("예상치 못한 예외 응답 전송 완료");
    }

    // 필터 제외
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return path.startsWith("/static") ||
                path.startsWith("/css") ||
                path.startsWith("/js") ||
                path.startsWith("/images") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars") ||
                path.startsWith("/actuator");
    }
}
