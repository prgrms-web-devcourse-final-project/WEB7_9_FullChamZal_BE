package back.fcz.global.security.filter;

import back.fcz.domain.sanction.service.IpBlockService;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import back.fcz.domain.sanction.util.RequestInfoExtractor;
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
import java.util.HashMap;
import java.util.Map;

// IP 차단 검증 필터
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlockFilter extends OncePerRequestFilter {

    private final IpBlockService ipBlockService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 클라이언트 IP 추출
        String clientIp = RequestInfoExtractor.extractIp(request);

        // IP 차단 여부 확인
        if (ipBlockService.isBlocked(clientIp)) {
            String reason = ipBlockService.getBlockReason(clientIp);
            log.warn("차단된 IP의 접근 시도: {} (사유: {})", clientIp, reason);

            // 403 Forbidden 응답
            sendBlockedResponse(response, reason);
            return;
        }

        // 정상 요청은 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    // IP 차단 응답 전송
    private void sendBlockedResponse(HttpServletResponse response, String reason) throws IOException {
        ErrorCode errorCode = ErrorCode.IP_BLOCKED;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, String> data = new HashMap<>();
        data.put("blockReason", reason != null ? reason : "보안 정책에 의해 차단되었습니다.");

        ApiResponse<Map<String, String>> apiResponse = new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                data
        );

        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }
}
