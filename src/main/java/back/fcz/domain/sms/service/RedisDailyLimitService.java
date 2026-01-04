package back.fcz.domain.sms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisDailyLimitService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private final StringRedisTemplate redisTemplate;

    /**
     * return:
     *  - 1..limit : 현재 카운트(허용)
     *  - 0        : limit 초과(차단)
     */
    private final DefaultRedisScript<Long> consumeScript = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            end
            if current > tonumber(ARGV[1]) then
              redis.call('DECR', KEYS[1])
              return 0
            end
            return current
            """,
            Long.class
    );

    public long consumeOrReject(String phoneNormalized, int dailyLimit) {
        String key = buildKey(phoneNormalized);
        long ttlSeconds = secondsUntilMidnightKst();

        Long result = redisTemplate.execute(
                consumeScript,
                List.of(key),
                String.valueOf(dailyLimit),
                String.valueOf(ttlSeconds)
        );
        return result == null ? 0 : result;
    }

    private String buildKey(String phoneNormalized) {
        String day = LocalDate.now(KST).format(DAY_FMT);
        return "sms:send:daily:" + day + ":" + phoneNormalized;
    }

    private long secondsUntilMidnightKst() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(KST);
        return Duration.between(now, midnight).getSeconds();
    }
}
