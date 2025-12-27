package back.fcz.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    // 서버 업로드 용 S3Client 빈 등록
    @Bean
    public S3Client s3Client(@Value("${app.s3.region}") String region){
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    // pre-signed URL 용 S3Client 빈 등록
    @Bean
    public S3Presigner s3Presigner(@Value("${app.s3.region}") String region){
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
