package back.fcz.global.util;

import back.fcz.domain.sanction.util.RequestInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestInfoExtractor 테스트")
class RequestInfoExtractorTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("X-Forwarded-For 헤더에서 IP를 추출한다")
    void extractIpFromXForwardedFor() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // when
        String ip = RequestInfoExtractor.extractIp(request);

        // then
        assertThat(ip).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("X-Forwarded-For에 여러 IP가 있으면 첫 번째 IP를 추출한다")
    void extractFirstIpFromXForwardedFor() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1, 192.0.2.1");

        // when
        String ip = RequestInfoExtractor.extractIp(request);

        // then
        assertThat(ip).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("X-Real-IP 헤더에서 IP를 추출한다")
    void extractIpFromXRealIp() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.2");

        // when
        String ip = RequestInfoExtractor.extractIp(request);

        // then
        assertThat(ip).isEqualTo("203.0.113.2");
    }

    @Test
    @DisplayName("헤더에 IP가 없으면 RemoteAddr에서 추출한다")
    void extractIpFromRemoteAddr() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED")).thenReturn(null);
        when(request.getHeader("HTTP_X_CLUSTER_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_FORWARDED_FOR")).thenReturn(null);
        when(request.getHeader("HTTP_FORWARDED")).thenReturn(null);
        when(request.getHeader("HTTP_VIA")).thenReturn(null);
        when(request.getHeader("REMOTE_ADDR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("203.0.113.3");

        // when
        String ip = RequestInfoExtractor.extractIp(request);

        // then
        assertThat(ip).isEqualTo("203.0.113.3");
    }

    @Test
    @DisplayName("unknown 값은 무시하고 다음 헤더를 확인한다")
    void ignoreUnknownValue() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.4");

        // when
        String ip = RequestInfoExtractor.extractIp(request);

        // then
        assertThat(ip).isEqualTo("203.0.113.4");
    }

    @Test
    @DisplayName("모든 헤더에서 IP를 찾지 못하면 UNKNOWN을 반환한다")
    void returnUnknownWhenNoValidIp() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED")).thenReturn(null);
        when(request.getHeader("HTTP_X_CLUSTER_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_FORWARDED_FOR")).thenReturn(null);
        when(request.getHeader("HTTP_FORWARDED")).thenReturn(null);
        when(request.getHeader("HTTP_VIA")).thenReturn(null);
        when(request.getHeader("REMOTE_ADDR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        // when
        String ip = RequestInfoExtractor.extractIp(request);

        // then
        assertThat(ip).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("request가 null이면 UNKNOWN을 반환한다")
    void returnUnknownWhenRequestIsNull() {
        // when
        String ip = RequestInfoExtractor.extractIp(null);

        // then
        assertThat(ip).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("User-Agent 헤더를 추출한다")
    void extractUserAgent() {
        // given
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        when(request.getHeader("User-Agent")).thenReturn(userAgent);

        // when
        String result = RequestInfoExtractor.extractUserAgent(request);

        // then
        assertThat(result).isEqualTo(userAgent);
    }

    @Test
    @DisplayName("User-Agent가 255자를 초과하면 잘라낸다")
    void truncateUserAgentOver255Chars() {
        // given
        String longUserAgent = "A".repeat(300);
        when(request.getHeader("User-Agent")).thenReturn(longUserAgent);

        // when
        String result = RequestInfoExtractor.extractUserAgent(request);

        // then
        assertThat(result).hasSize(255);
        assertThat(result).isEqualTo("A".repeat(255));
    }

    @Test
    @DisplayName("User-Agent가 없으면 UNKNOWN을 반환한다")
    void returnUnknownWhenNoUserAgent() {
        // given
        when(request.getHeader("User-Agent")).thenReturn(null);

        // when
        String result = RequestInfoExtractor.extractUserAgent(request);

        // then
        assertThat(result).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("User-Agent가 빈 문자열이면 UNKNOWN을 반환한다")
    void returnUnknownWhenUserAgentIsBlank() {
        // given
        when(request.getHeader("User-Agent")).thenReturn("   ");

        // when
        String result = RequestInfoExtractor.extractUserAgent(request);

        // then
        assertThat(result).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("request가 null이면 User-Agent는 UNKNOWN을 반환한다")
    void returnUnknownWhenRequestIsNullForUserAgent() {
        // when
        String result = RequestInfoExtractor.extractUserAgent(null);

        // then
        assertThat(result).isEqualTo("UNKNOWN");
    }
}