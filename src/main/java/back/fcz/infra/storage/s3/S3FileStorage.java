package back.fcz.infra.storage.s3;


import back.fcz.infra.storage.FileStorage;
import back.fcz.infra.storage.FileUploadCommand;
import back.fcz.infra.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor

public class S3FileStorage implements FileStorage {
    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Override
    public StoredFile store(FileUploadCommand command, String key) {


        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(command.contentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(command.inputStream(), command.size()));

        return new StoredFile(
                key,
                command.filename(),
                command.contentType(),
                command.size()
        );
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
