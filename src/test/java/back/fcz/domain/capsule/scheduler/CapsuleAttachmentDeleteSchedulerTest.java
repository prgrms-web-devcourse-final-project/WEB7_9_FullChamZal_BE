package back.fcz.domain.capsule.scheduler;

import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.entity.CapsuleAttachmentStatus;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.infra.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CapsuleAttachmentDeleteSchedulerTest {

    @InjectMocks
    private CapsuleAttachmentDeleteScheduler scheduler;

    @Mock
    private CapsuleAttachmentRepository repository;

    @Mock
    private FileStorage fileStorage;

    @Test
    @DisplayName("만료된 TEMP → DELETED 마킹")
    void markExpiredTempAsDeleted_success() {
        CapsuleAttachment a1 = mock(CapsuleAttachment.class);
        CapsuleAttachment a2 = mock(CapsuleAttachment.class);

        given(repository.findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
                eq(CapsuleAttachmentStatus.TEMP), any(LocalDateTime.class)
        )).willReturn(List.of(a1, a2));

        scheduler.markExpiredTempAsDeleted();

        verify(a1).markDeleted();
        verify(a2).markDeleted();
        verify(repository).saveAll(List.of(a1, a2));
    }

    @Test
    @DisplayName("DELETED 상태 S3 삭제 후 DB 삭제")
    void purgeDeletedFromS3_success() {
        CapsuleAttachment a1 = mock(CapsuleAttachment.class);
        given(a1.getS3Key()).willReturn("capsules/10/a.png");

        CapsuleAttachment a2 = mock(CapsuleAttachment.class);
        given(a2.getS3Key()).willReturn("capsules/10/b.png");

        given(repository.findTop1000ByStatusOrderByIdAsc(CapsuleAttachmentStatus.DELETED))
                .willReturn(List.of(a1, a2));

        scheduler.hardDeletedFromS3();

        verify(fileStorage).delete("capsules/10/a.png");
        verify(fileStorage).delete("capsules/10/b.png");
        verify(repository).delete(a1);
        verify(repository).delete(a2);
    }
}
