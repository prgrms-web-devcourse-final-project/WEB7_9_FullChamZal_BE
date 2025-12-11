package back.fcz.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private TokenConfig accessToken = new TokenConfig();

    private TokenConfig refreshToken = new TokenConfig();

    @Getter
    @Setter
    public static class TokenConfig {
        private long expiration;
    }
}
