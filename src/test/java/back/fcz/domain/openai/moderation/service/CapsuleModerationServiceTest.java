package back.fcz.domain.openai.moderation.service;

import back.fcz.domain.openai.moderation.client.OpenAiModerationClient;
import back.fcz.domain.openai.moderation.dto.CapsuleModerationBlockedPayload;
import back.fcz.domain.openai.moderation.dto.ModerationField;
import back.fcz.domain.openai.moderation.dto.OpenAiModerationResult;
import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.entity.ModerationAuditLog;
import back.fcz.domain.openai.moderation.entity.ModerationDecision;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapsuleModerationServiceTest {

    @Mock
    OpenAiModerationClient openAiModerationClient;

    @Mock
    ModerationAuditLogWriter auditLogWriter;

    ObjectMapper objectMapper = new ObjectMapper();

    CapsuleModerationService service;

    @BeforeEach
    void setUp() {
        service = new CapsuleModerationService(openAiModerationClient, auditLogWriter, objectMapper);
        lenient().when(openAiModerationClient.getModel()).thenReturn("omni-moderation-2024-09-26");
    }

    @Test
    void 입력이_비면_SKIPPED_취급으로_DB저장_없이_null_반환() {
        Long auditId = service.validateCapsuleText(
                1L,
                ModerationActionType.CAPSULE_CREATE,
                "   ",
                null,
                "",
                "   ",
                null
        );

        assertThat(auditId).isNull();
        verifyNoInteractions(openAiModerationClient);
        verifyNoInteractions(auditLogWriter);
    }

    @Test
    void PASS면_DB저장_없이_null_반환() {
        // combinedInput = "CONTENT: ok"
        OpenAiModerationResult overallPass = new OpenAiModerationResult(
                false,
                List.of(),
                objectMapper.createObjectNode()
        );

        when(openAiModerationClient.moderateText("CONTENT: ok")).thenReturn(overallPass);

        Long auditId = service.validateCapsuleText(
                1L,
                ModerationActionType.CAPSULE_CREATE,
                null,
                "ok",
                null,
                null,
                null
        );

        assertThat(auditId).isNull();
        verify(auditLogWriter, never()).saveAndReturnId(any());
    }

    @Test
    void FLAGGED면_DB에_로그_저장하고_CAPSULE_CONTENT_BLOCKED_예외() {
        // combinedInput = "CONTENT: bad"
        ObjectNode overallRaw = objectMapper.createObjectNode().put("flagged", true);
        OpenAiModerationResult overallFlagged = new OpenAiModerationResult(
                true,
                List.of("hate"),
                overallRaw
        );

        ObjectNode contentRaw = objectMapper.createObjectNode().put("flagged", true);
        OpenAiModerationResult contentFlagged = new OpenAiModerationResult(
                true,
                List.of("hate"),
                contentRaw
        );

        when(openAiModerationClient.moderateText("CONTENT: bad")).thenReturn(overallFlagged);
        when(openAiModerationClient.moderateText("bad")).thenReturn(contentFlagged);

        when(openAiModerationClient.toRawJson(any())).thenReturn("{\"mock\":\"raw\"}");
        when(auditLogWriter.saveAndReturnId(any())).thenReturn(100L);

        BusinessException ex = catchThrowableOfType(() -> service.validateCapsuleText(
                7L,
                ModerationActionType.CAPSULE_CREATE,
                null,
                "bad",
                null,
                null,
                null
        ), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CAPSULE_CONTENT_BLOCKED);
        assertThat(ex.getMessage()).contains("유해한 내용");

        assertThat(ex.getData()).isInstanceOf(CapsuleModerationBlockedPayload.class);
        CapsuleModerationBlockedPayload payload = (CapsuleModerationBlockedPayload) ex.getData();

        assertThat(payload.getAuditId()).isEqualTo(100L);
        assertThat(payload.getViolations()).isNotEmpty();
        assertThat(payload.getViolations().get(0).getField()).isEqualTo(ModerationField.CONTENT);

        ArgumentCaptor<ModerationAuditLog> captor = ArgumentCaptor.forClass(ModerationAuditLog.class);
        verify(auditLogWriter).saveAndReturnId(captor.capture());

        ModerationAuditLog savedLog = captor.getValue();
        assertThat(savedLog.getDecision()).isEqualTo(ModerationDecision.FLAGGED);
        assertThat(savedLog.isFlagged()).isTrue();
        assertThat(savedLog.getInputHash()).isNotBlank();
    }

    @Test
    void OpenAI_호출_실패하면_ERROR로그_저장하고_OPENAI_MODERATION_FAILED_예외() {
        when(openAiModerationClient.moderateText("CONTENT: ok"))
                .thenThrow(new RestClientException("timeout"));

        when(auditLogWriter.saveAndReturnId(any())).thenReturn(999L);

        BusinessException ex = catchThrowableOfType(() -> service.validateCapsuleText(
                1L,
                ModerationActionType.CAPSULE_CREATE,
                null,
                "ok",
                null,
                null,
                null
        ), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OPENAI_MODERATION_FAILED);
        assertThat(ex.getData()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) ex.getData();
        assertThat(payload.get("auditId")).isEqualTo(999L);
        assertThat(payload.get("reason")).isEqualTo("OPENAI_CALL_FAILED");

        ArgumentCaptor<ModerationAuditLog> captor = ArgumentCaptor.forClass(ModerationAuditLog.class);
        verify(auditLogWriter).saveAndReturnId(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo(ModerationDecision.ERROR);
    }
}
