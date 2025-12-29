package back.fcz.global.security.jwt.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";

    // 쿠키 경로
    private static final String COOKIE_PATH = "/";

    // 쿠키 생성
    public static Cookie createCookie(String name, String value, int maxAgeSeconds, boolean isSecure, String sameSite) {
        Cookie cookie = new Cookie(name, value);

        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath(COOKIE_PATH);
        cookie.setHttpOnly(true); // XSS 방어
        cookie.setSecure(isSecure); // 중간자 공격 방어 (개발 환경에서는 false, 운영 환경에서는 true)
        cookie.setAttribute("SameSite", sameSite);

        log.debug("쿠키 생성 완료. name: {}, maxAge: {}초, secure: {}, sameSite: {}",
                name, maxAgeSeconds, isSecure, sameSite);

        return cookie;
    }

    // 쿠키 조회
    public static Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null || cookies.length == 0) {
            log.debug("요청에 쿠키가 없습니다.");
            return Optional.empty();
        }

        Optional<String> cookieValue = Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();

        if (cookieValue.isPresent()) {
            log.debug("쿠키 조회 성공. name: {}, value: {}...",
                    cookieName, cookieValue.get().substring(0, Math.min(20, cookieValue.get().length())));
        } else {
            log.debug("쿠키를 찾을 수 없습니다. name: {}", cookieName);
        }

        return cookieValue;
    }

    // 로그아웃용 쿠키 삭제
    public static void deleteAllTokenCookies(HttpServletResponse response, boolean isSecure, String sameSite, String domain) {
        deleteCookie(response, ACCESS_TOKEN_COOKIE, isSecure, sameSite, domain);
        deleteCookie(response, REFRESH_TOKEN_COOKIE, isSecure, sameSite, domain);
        log.info("모든 토큰 쿠키 삭제 완료");
    }

    // 쿠키 삭제
    public static void deleteCookie(HttpServletResponse response, String cookieName, boolean isSecure, String sameSite, String domain) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .domain(domain)
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.info("쿠키 삭제 완료. name: {}", cookieName);
    }
}
