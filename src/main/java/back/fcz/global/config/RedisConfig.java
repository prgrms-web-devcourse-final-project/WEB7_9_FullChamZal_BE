package back.fcz.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:3000ms}")
    private String redisTimeout;

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = "redis://" + redisHost + ":" + redisPort;

        int timeoutMs = parseTimeoutToMs(redisTimeout);

        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionPoolSize(50)  // 연결 풀 크기
                .setConnectionMinimumIdleSize(10)  // 최소 유휴 연결
                .setConnectTimeout(timeoutMs)  // 연결 타임아웃
                .setTimeout(timeoutMs)  // 명령 응답 타임아웃
                .setRetryAttempts(3)  // 재시도 횟수
                .setRetryInterval(1500);  // 재시도 간격 (ms)

        return Redisson.create(config);
    }

    private int parseTimeoutToMs(String timeout) {
        try {
            if (timeout.endsWith("ms")) {
                return Integer.parseInt(timeout.replace("ms", ""));
            } else if (timeout.endsWith("s")) {
                return Integer.parseInt(timeout.replace("s", "")) * 1000;
            } else {
                // 단위가 없으면 밀리초로 간주
                return Integer.parseInt(timeout);
            }
        } catch (NumberFormatException e) {
            // 파싱 실패 시 기본값 3000ms
            return 3000;
        }
    }
}
