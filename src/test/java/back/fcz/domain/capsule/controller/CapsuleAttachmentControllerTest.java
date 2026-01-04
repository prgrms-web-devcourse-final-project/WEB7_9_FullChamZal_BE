package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.service.AttachmentService;
import back.fcz.domain.capsule.service.CapsuleAttachmentPresignService;
import back.fcz.domain.capsule.service.CapsuleAttachmentServerUploadService;
import back.fcz.domain.member.service.CurrentUserContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class CapsuleAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CapsuleAttachmentPresignService capsuleAttachmentPresignService;

    @MockitoBean
    private CapsuleAttachmentServerUploadService capsuleAttachmentServerUploadService;

    @MockitoBean
    private CurrentUserContext currentUserContext;

    @MockitoBean
    private AttachmentService attachmentService;

    @MockitoBean
    private RedissonClient redissonClient;

    @Test
    @DisplayName("파일 서버 업로드 API 성공")
    void uploadByServer_success() throws Exception {
        given(currentUserContext.getCurrentMemberId()).willReturn(10L);

        given(capsuleAttachmentServerUploadService.uploadTemp(eq(10L), any(MultipartFile.class)))
                .willReturn(new CapsuleAttachmentUploadResponse(1L, null, null, null));

        mockMvc.perform(
                        multipart("/api/v1/capsule/upload")
                                .file(new MockMultipartFile("file", "a.png", "image/png", "x".getBytes()))
                )
                .andExpect(status().isOk());

        verify(capsuleAttachmentServerUploadService).uploadTemp(eq(10L), any(MultipartFile.class));
    }

    @Test
    @DisplayName("임시 파일 삭제 API 성공")
    void deleteTemp_success() throws Exception {
        given(currentUserContext.getCurrentMemberId()).willReturn(10L);

        mockMvc.perform(delete("/api/v1/capsule/upload/{attachmentId}", 1L))
                .andExpect(status().isOk());

        verify(attachmentService).deleteTemp(10L, 1L);
    }
}
