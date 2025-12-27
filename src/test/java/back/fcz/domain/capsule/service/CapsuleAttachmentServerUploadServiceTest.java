package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.infra.storage.FileStorage;
import back.fcz.infra.storage.FileUploadCommand;
import back.fcz.infra.storage.StoredFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CapsuleAttachmentServerUploadServiceTest {

    @InjectMocks
    private CapsuleAttachmentServerUploadService service;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private CapsuleAttachmentRepository capsuleAttachmentRepository;

    @Test
    @DisplayName("서버 업로드 방식 - TEMP 업로드 성공")
    void uploadTemp_success() throws Exception {
        // given
        Long uploaderId = 10L;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "dummy".getBytes()
        );

        // fileStorage.store 결과
        StoredFile storedFile = new StoredFile(
                "capsules/10/uuid.png",
                "test.png",
                "image/png",
                (long) file.getBytes().length
        );
        given(fileStorage.store(any(FileUploadCommand.class), anyString()))
                .willReturn(storedFile);

        // save 시 id 세팅을 흉내 (JPA가 해주는 걸 테스트에서 재현)
        given(capsuleAttachmentRepository.save(any(CapsuleAttachment.class)))
                .willAnswer(inv -> {
                    CapsuleAttachment a = inv.getArgument(0);
                    // 리플렉션으로 id 세팅 (엔티티에 setter 없을 때)
                    Field idField = CapsuleAttachment.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(a, 1L);
                    return a;
                });

        // when
        CapsuleAttachmentUploadResponse res = service.uploadTemp(uploaderId, file);

        // then
        verify(fileStorage).store(any(FileUploadCommand.class), anyString());
        verify(capsuleAttachmentRepository).save(any(CapsuleAttachment.class));

        assertThat(res.attachmentId()).isEqualTo(1L);
    }
}
