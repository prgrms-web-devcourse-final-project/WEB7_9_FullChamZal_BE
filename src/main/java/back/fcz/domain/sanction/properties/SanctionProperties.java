package back.fcz.domain.sanction.properties;

import back.fcz.domain.capsule.entity.AnomalyType;
import back.fcz.domain.sanction.constant.RiskLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "sanction")
public class SanctionProperties {

    private RateLimit rateLimit = new RateLimit();
    private Monitoring monitoring = new Monitoring();

    @Getter
    @Setter
    public static class RateLimit {
        private Map<RiskLevel, Integer> windowSeconds;
        private Map<RiskLevel, Integer> maxRequests;
        private Map<RiskLevel, Integer> cooldownSeconds;
    }

    @Getter
    @Setter
    public static class Monitoring {
        private Thresholds thresholds = new Thresholds();
        private Map<AnomalyType, Integer> anomalyScores;
        private Duration suspicionTtl;

        @Getter
        @Setter
        public static class Thresholds {
            private int warning;
            private int limit;
        }
    }
}
