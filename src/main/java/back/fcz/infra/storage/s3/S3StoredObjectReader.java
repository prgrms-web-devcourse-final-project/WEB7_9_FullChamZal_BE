package back.fcz.infra.storage.s3;

import back.fcz.infra.storage.StoredObjectMetadata;
import back.fcz.infra.storage.StoredObjectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Service
@RequiredArgsConstructor
public class S3StoredObjectReader implements StoredObjectReader {

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Override
    public StoredObjectMetadata head(String s3Key) {
        try{
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .build()
            );

            return new StoredObjectMetadata(
                    s3Key,
                    response.contentType(),
                    response.contentLength()
            );
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("S3 객체를 찾을 수 없습니다: " + s3Key, e);
        }
    }
}
