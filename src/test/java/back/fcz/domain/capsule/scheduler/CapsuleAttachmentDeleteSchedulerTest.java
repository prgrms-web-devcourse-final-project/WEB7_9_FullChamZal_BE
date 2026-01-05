package back.fcz.domain.capsule.scheduler;

import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.entity.CapsuleAttachmentStatus;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.infra.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapsuleAttachmentDeleteSchedulerTest {

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
                eq(CapsuleAttachmentStatus.TEMP),
                any(LocalDateTime.class)
        )).willReturn(List.of(a1, a2));

        scheduler.markExpiredTempAsDeleted();

        verify(a1).markDeleted();
        verify(a2).markDeleted();
        verify(repository).saveAll(List.of(a1, a2));
    }

    @Test
    @DisplayName("만료된 UPLOADING / PENDING_MODERATION → DELETED 마킹")
    void markExpiredUploadingOrPendingAsDeleted_success() {
        CapsuleAttachment u1 = mock(CapsuleAttachment.class);
        CapsuleAttachment p1 = mock(CapsuleAttachment.class);
        CapsuleAttachment p2 = mock(CapsuleAttachment.class);

        // UPLOADING 만료 조회
        given(repository.findTop1000ByStatusAndCreatedAtBeforeOrderByIdAsc(
                eq(CapsuleAttachmentStatus.UPLOADING),
                any(LocalDateTime.class)
        )).willReturn(List.of(u1));

        // PENDING_MODERATION 만료 조회 (updatedAt 기준 예시)
        given(repository.findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
                eq(CapsuleAttachmentStatus.PENDING),
                any(LocalDateTime.class)
        )).willReturn(List.of(p1, p2));

        scheduler.markExpiredUploadingOrPendingAsDeleted();

        verify(u1).markDeleted();
        verify(p1).markDeleted();
        verify(p2).markDeleted();

        verify(repository).saveAll(List.of(u1));
        verify(repository).saveAll(List.of(p1, p2));
    }

    @Test
    @DisplayName("DELETED 상태: S3 삭제 성공한 것만 DB 배치 하드삭제")
    void hardDeletedFromS3_success_batchDeleteOnlySuccess() {
        CapsuleAttachment a1 = mock(CapsuleAttachment.class);
        given(a1.getId()).willReturn(1L);
        given(a1.getS3Key()).willReturn("capsules/10/a.png");

        CapsuleAttachment a2 = mock(CapsuleAttachment.class);
        given(a2.getId()).willReturn(2L);
        given(a2.getS3Key()).willReturn("capsules/10/b.png");

        // deletedAt 오래된 것부터 가져오는 쿼리로 변경된 케이스
        given(repository.findTop1000ByStatusOrderByDeletedAtAsc(CapsuleAttachmentStatus.DELETED))
                .willReturn(List.of(a1, a2));

        // a2 S3 삭제 실패 시나리오
        doNothing().when(fileStorage).delete("capsules/10/a.png");
        doThrow(new RuntimeException("S3 error")).when(fileStorage).delete("capsules/10/b.png");

        scheduler.hardDeletedFromS3();

        verify(fileStorage).delete("capsules/10/a.png");
        verify(fileStorage).delete("capsules/10/b.png");

        // ✅ 성공한 a1만 배치 삭제
        verify(repository).deleteAllByIdInBatch(List.of(1L));

        // ❌ 실패한 a2는 DB 삭제하면 안 됨
        verify(repository, never()).deleteAllByIdInBatch(List.of(2L));
    }

    @Test
    @DisplayName("DELETED 대상이 없으면 아무 것도 하지 않음")
    void hardDeletedFromS3_empty_noop() {
        given(repository.findTop1000ByStatusOrderByDeletedAtAsc(CapsuleAttachmentStatus.DELETED))
                .willReturn(List.of());

        scheduler.hardDeletedFromS3();

        verifyNoInteractions(fileStorage);
        verify(repository, never()).deleteAllByIdInBatch(anyList());
    }
}
