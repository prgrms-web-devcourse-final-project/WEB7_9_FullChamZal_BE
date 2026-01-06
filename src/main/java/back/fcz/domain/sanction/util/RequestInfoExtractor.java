package back.fcz.domain.sanction.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

// 실제 클라이언트 IP 추출
@Slf4j
public class RequestInfoExtractor {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    // HTTP 요청에서 실제 클라이언트 IP 주소 추출
    public static String extractIp(HttpServletRequest request) {
        if(request == null) {
            return "UNKNOWN"; // 추출 실패 시
        }

        // 헤더 우선순위대로 확인
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For는 "client, proxy1, proxy2" 형식일 수 있으므로 첫 번째 IP만 추출
                if ("X-Forwarded-For".equalsIgnoreCase(header) && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                log.debug("IP 추출 성공: {} = {}", header, ip);
                return ip;
            }
        }

        // 헤더에서 못 찾으면 RemoteAddr 사용
        String remoteAddr = request.getRemoteAddr();
        if (isValidIp(remoteAddr)) {
            log.debug("IP 추출 성공: RemoteAddr = {}", remoteAddr);
            return remoteAddr;
        }

        log.warn("유효한 IP 추출 실패, UNKNOWN 반환");
        return "UNKNOWN";
    }

    // HTTP 요청에서 User-Agent(브라우저 정보) 헤더를 추출
    public static String extractUserAgent(HttpServletRequest request) {
        if(request == null) {
            return "UNKNOWN";
        }

        String userAgent = request.getHeader("User-Agent");
        if(userAgent != null && !userAgent.isBlank()) {
            if (userAgent.length() > 255) {
                userAgent = userAgent.substring(0, 255);
            }

            return userAgent;
        }

        return "UNKNOWN";
    }

    // 유효한 IP인지 검증하는 헬퍼 메서드
    private static boolean isValidIp(String ip) {
        return ip != null
                && !ip.isBlank()
                && !ip.isBlank()
                && !"unknown".equalsIgnoreCase(ip);
    }
}
