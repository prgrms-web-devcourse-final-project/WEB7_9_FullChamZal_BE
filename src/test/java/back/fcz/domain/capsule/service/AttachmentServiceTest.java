package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentViewResponse;
import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.entity.CapsuleAttachmentStatus;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.infra.storage.FileStorage;
import back.fcz.infra.storage.PresignedUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttachmentServiceTest {

    @InjectMocks
    private AttachmentService service;

    @Mock
    private CapsuleAttachmentRepository capsuleAttachmentRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private PresignedUrlProvider presignedUrlProvider;

    @Test
    @DisplayName("presignedDownload - URL 발급 성공")
    void presignedDownload_success() {
        Long memberId = 10L;
        Long attachmentId = 1L;

        CapsuleAttachment attachment = mock(CapsuleAttachment.class);
        given(attachment.getS3Key()).willReturn("capsules/10/uuid.png");

        given(capsuleAttachmentRepository.findById(attachmentId))
                .willReturn(Optional.of(attachment));

        given(presignedUrlProvider.presignGet(eq("capsules/10/uuid.png"), any(Duration.class)))
                .willReturn("https://get-url");

        CapsuleAttachmentViewResponse url = service.presignedDownload(memberId, attachmentId);

        assertThat(url.presignedUrl()).isEqualTo("https://get-url");
        verify(presignedUrlProvider).presignGet(eq("capsules/10/uuid.png"), any(Duration.class));
    }
    @Test
    @DisplayName("deleteTemp - TEMP 상태면 DELETED 마킹")
    void deleteTemp_success_marksDeleted() {
        Long memberId = 10L;
        Long attachmentId = 1L;

        CapsuleAttachment attachment = mock(CapsuleAttachment.class);
        given(attachment.getStatus()).willReturn(CapsuleAttachmentStatus.TEMP);

        given(capsuleAttachmentRepository.findByIdAndUploaderId(attachmentId, memberId))
                .willReturn(Optional.of(attachment));

        service.deleteTemp(memberId, attachmentId);

        verify(attachment).markDeleted();
        verify(capsuleAttachmentRepository).save(attachment);

        // 정책에 따라:
        // - 스케줄러 삭제면 never()
        verify(fileStorage, never()).delete(anyString());
    }
}



