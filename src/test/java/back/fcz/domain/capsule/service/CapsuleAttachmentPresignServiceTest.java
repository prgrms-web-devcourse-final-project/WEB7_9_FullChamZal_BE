package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleAttachmentUploadRequest;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.infra.storage.PresignedUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CapsuleAttachmentPresignServiceTest {

    @InjectMocks
    private CapsuleAttachmentPresignService service;

    @Mock
    private PresignedUrlProvider presignedUrlProvider;

    @Mock
    private CapsuleAttachmentRepository capsuleAttachmentRepository;

    @Test
    @DisplayName("Presigned 업로드 - TEMP 메타 저장 + PUT URL 발급 성공")
    void presignedUpload_success() throws Exception {
        Long uploaderId = 10L;
        CapsuleAttachmentUploadRequest req =
                new CapsuleAttachmentUploadRequest("a.png", "image/png", 123L);

        given(presignedUrlProvider.presignPut(anyString(), eq("image/png"), eq(123L), any(Duration.class)))
                .willReturn("https://put-url");

        given(capsuleAttachmentRepository.save(any(CapsuleAttachment.class)))
                .willAnswer(inv -> {
                    CapsuleAttachment a = inv.getArgument(0);
                    Field idField = CapsuleAttachment.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(a, 7L);
                    return a;
                });

        CapsuleAttachmentUploadResponse res = service.presignedUpload(uploaderId, req);

        verify(capsuleAttachmentRepository).save(any(CapsuleAttachment.class));
        verify(presignedUrlProvider).presignPut(anyString(), eq("image/png"), eq(123L), any(Duration.class));

        assertThat(res.attachmentId()).isEqualTo(7L);
        assertThat(res.presignedUrl()).isEqualTo("https://put-url");
        assertThat(res.s3Key()).isNotBlank();
    }
}
