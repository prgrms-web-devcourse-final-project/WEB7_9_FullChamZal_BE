package back.fcz.infra.storage;

import java.time.Duration;

public interface PresignedUrlProvider {
    String presignPut(String key, String contentType, long size, Duration expires);
    String presignGet(String key, Duration expires);
}
