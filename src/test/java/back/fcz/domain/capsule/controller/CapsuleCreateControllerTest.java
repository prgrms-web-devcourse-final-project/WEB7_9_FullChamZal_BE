package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.GPSResponseDTO;
import back.fcz.domain.capsule.DTO.LetterCustomResponseDTO;
import back.fcz.domain.capsule.DTO.UnlockResponseDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleUpdateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleUpdateResponseDTO;
import back.fcz.domain.capsule.DTO.response.SecretCapsuleCreateResponseDTO;
import back.fcz.domain.capsule.service.CapsuleCreateService;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class CapsuleCreateControllerTest {

    @InjectMocks
    private CapsuleCreateController controller;

    @Mock
    private CapsuleCreateService capsuleCreateService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("캡슐 생성 시, phoneNum == null이면 비밀번호 방식 실행")
    void testPrivateCapsuleCreate_PasswordMode() throws Exception {

        SecretCapsuleCreateResponseDTO mockResponse =
                new SecretCapsuleCreateResponseDTO(
                        1L, 1L, "nick", "http://url",
                        "1234", "title", "content",
                        "PRIVATE", "TIME",
                        null, null, 10, 0
                );

        Mockito.when(capsuleCreateService.privateCapsulePassword(any(), any()))
                .thenReturn(mockResponse);

        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "nick", "title", "content",
                "PRIVATE", "TIME", LocalDateTime.now(),
                "Seoul", 37.1, 127.2,
                300, "red", "white", 10
        );

        mockMvc.perform(
                        post("/api/v1/capsule/create/private")
                                .param("capsulePassword", "1234")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk());

        Mockito.verify(capsuleCreateService, Mockito.times(1))
                .privateCapsulePassword(any(), eq("1234"));
    }

    // capsulePassword == null → 전화번호 방식 실행
    @Test
    @DisplayName("캡슐 생성 시, capsulePassword == null이면 전화번호 방식 실행")
    void testPrivateCapsuleCreate_PhoneMode() throws Exception {

        SecretCapsuleCreateResponseDTO mockResponse =
                new SecretCapsuleCreateResponseDTO(
                        1L, 1L, "nick",
                        "http://url", null,
                        "title", "content",
                        "PRIVATE", "TIME",
                        null, null, 10, 0
                );

        Mockito.when(capsuleCreateService.privateCapsulePhone(any(), any()))
                .thenReturn(mockResponse);

        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "nick", "title", "content",
                "PRIVATE", "TIME",
                LocalDateTime.now(),
                "Seoul", 37.1, 127.2,
                300, "red", "white", 10
        );

        mockMvc.perform(
                        post("/api/v1/capsule/create/private")
                                .param("phoneNum", "01012341234")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk());

        Mockito.verify(capsuleCreateService, Mockito.times(1))
                .privateCapsulePhone(any(), eq("01012341234"));
    }

    @Test
    @DisplayName("캡슐 생성 시, phoneNum과 capsulePassword가 둘 다 존재하면 예외 발생")
    void testPrivateCapsuleCreate_BothProvided_ShouldThrow() throws Exception {

        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "nick", "title", "content",
                "PRIVATE", "TIME",
                LocalDateTime.now(),
                "Seoul", 37.1, 127.2,
                300, "red", "white", 10
        );

        mockMvc.perform(
                        post("/api/v1/capsule/create/private")
                                .param("phoneNum", "01012341234")
                                .param("capsulePassword", "9999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    assertInstanceOf(BusinessException.class, result.getResolvedException());
                })
                .andExpect(result -> {
                    BusinessException ex = (BusinessException) result.getResolvedException();
                    assertEquals(ErrorCode.CAPSULE_NOT_CREATE, ex.getErrorCode());
                });

        Mockito.verify(capsuleCreateService, Mockito.never()).privateCapsulePhone(any(), any());
        Mockito.verify(capsuleCreateService, Mockito.never()).privateCapsulePassword(any(), any());
    }

    // 캡슐 수정 테스트
    @Test
    @DisplayName("캡슐 수정 요청이 성공하면 200 OK 와 수정된 데이터가 반환된다")
    void updateCapsule_success() throws Exception {

        // given
        Long capsuleId = 1L;

        CapsuleUpdateRequestDTO requestDTO =
                new CapsuleUpdateRequestDTO("new title", "new content");

        CapsuleUpdateResponseDTO responseDTO =
                new CapsuleUpdateResponseDTO(
                        10L,              // memberId
                        capsuleId,        // capsuleId
                        "닉네임",
                        "new title",
                        "new content",
                        "PUBLIC",
                        "TIME",
                        new UnlockResponseDTO(
                                LocalDateTime.now(),
                                "장소",
                                new GPSResponseDTO(37.5, 127.0),
                                100
                        ),
                        new LetterCustomResponseDTO("BLUE", "RED"),
                        10,
                        0
                );

        Mockito.when(capsuleCreateService.updateCapsule(eq(capsuleId), any()))
                .thenReturn(responseDTO);

        // when & then
        mockMvc.perform(put("/api/v1/capsule/update")
                        .param("capsuleId", capsuleId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capsuleId").value(1))
                .andExpect(jsonPath("$.updatedTitle").value("new title"))
                .andExpect(jsonPath("$.updatedContent").value("new content"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.unlock.location").value("장소"))
                .andExpect(jsonPath("$.letter.capsuleColor").value("BLUE"));
    }
}
