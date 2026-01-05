package back.fcz.infra.storage.s3;

import back.fcz.infra.storage.PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlProvider implements PresignedUrlProvider {

    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Override
    public String presignPut(String key, String contentType, long size, Duration expires) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();

        return s3Presigner.presignPutObject(p-> p
                .signatureDuration(expires)
                .putObjectRequest(request))
                .url()
                .toString();
    }

    public String presignGet(String key, Duration expires) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Presigner.presignGetObject(p -> p
                .signatureDuration(expires)
                .getObjectRequest(request))
                .url()
                .toString();
    }
}
