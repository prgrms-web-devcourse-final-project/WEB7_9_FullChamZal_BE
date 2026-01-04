package back.fcz.global.security.filter;

import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.service.MemberStatusCache;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;
    private final MemberStatusCache memberStatusCache;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

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

            MemberStatus status = memberStatusCache.getStatus(memberId);

            if (status == MemberStatus.STOP) {
                log.warn("정지된 회원의 접근 시도: memberId={}, status=STOP, URI={}",
                        memberId, request.getRequestURI());
                throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
            }

            if (status == MemberStatus.EXIT) {
                log.warn("탈퇴한 회원의 접근 시도: memberId={}, status=EXIT, URI={}",
                        memberId, request.getRequestURI());
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
            }

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
        ErrorCode errorCode = e.getErrorCode();

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(errorCode);

        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);

        log.info("인증 예외 응답 전송 완료 - ErrorCode: {}", e.getErrorCode());
    }

    // 500 에러 - 서버 오류
    private void handleUnexpectedException(HttpServletResponse response, Exception e) throws IOException {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(errorCode);

        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);

        log.error("예상치 못한 예외 응답 전송 완료");
    }

    // 필터 제외
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 1. 정적 리소스 제외
        if (path.startsWith("/static") ||
                path.startsWith("/css") ||
                path.startsWith("/js") ||
                path.startsWith("/images") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars") ||
                path.startsWith("/actuator") ||
                path.startsWith("/h2-console")) {
            return true;
        }

        // 2. 인증 생성/갱신 API 제외 (JWT 검증 자체가 불필요)
        if (path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/signup") ||
                path.equals("/api/v1/auth/refresh") ||
                path.equals("/api/v1/auth/findUserId") ||
                path.equals("/api/v1/auth/findPassword") ||
                path.startsWith("/oauth2/authorization/google")) {
            return true;
        }

        return false;
    }
}
