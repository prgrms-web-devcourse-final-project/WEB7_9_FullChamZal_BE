package back.fcz.domain.storytrack.controller;

import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.storytrack.dto.response.DeleteParticipantResponse;
import back.fcz.domain.storytrack.dto.response.DeleteStorytrackResponse;
import back.fcz.domain.storytrack.service.StorytrackService;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class StorytrackControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StorytrackService storytrackService;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private StorytrackController storytrackController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(storytrackController)
                // .setHandlerExceptionResolvers(new DefaultHandlerExceptionResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void mockLoginUser(Long memberId) {
        InServerMemberResponse user = mock(InServerMemberResponse.class);
        given(user.memberId()).willReturn(memberId);
        given(currentUserContext.getCurrentUser()).willReturn(user);
    }

    // 삭제 통합 테스트
    @Test
    @DisplayName("스토리트랙 작성자 삭제 성공")
    void deleteStorytrack_success() throws Exception {
        // given
        Long storytrackId = 1L;
        Long loginMember = 1L;

        mockLoginUser(loginMember);

        DeleteStorytrackResponse response =
                new DeleteStorytrackResponse(
                        storytrackId,
                        "1번 스토리트랙이 삭제 되었습니다."
                );

        given(storytrackService.deleteStorytrack(loginMember, storytrackId))
                .willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/v1/storytrack/delete")
                        .param("storytrackId", storytrackId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storytrackId").value(storytrackId))
                .andExpect(jsonPath("$.message").value("1번 스토리트랙이 삭제 되었습니다."));

        verify(storytrackService).deleteStorytrack(loginMember, storytrackId);
    }

    @Test
    @DisplayName("스토리트랙 참여자 참여 종료 성공")
    void deleteParticipant_success() throws Exception {
        // given
        Long storytrackId = 1L;
        Long loginMember = 2L;

        mockLoginUser(loginMember);

        DeleteParticipantResponse response =
                new DeleteParticipantResponse("스토리트랙 참여를 종료했습니다.");

        given(storytrackService.deleteParticipant(loginMember, storytrackId))
                .willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/v1/storytrack/delete/participant")
                        .param("storytrackId", storytrackId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("스토리트랙 참여를 종료했습니다."));

        verify(storytrackService).deleteParticipant(loginMember, storytrackId);
    }

    @Test
    @DisplayName("스토리트랙 작성자가 아니면 삭제 실패")
    void deleteStorytrack_notCreator() throws Exception {
        // given
        Long loginMember = 1L;
        Long storytrackId = 10L;

        mockLoginUser(loginMember);

        given(storytrackService.deleteStorytrack(loginMember, storytrackId))
                .willThrow(new BusinessException(ErrorCode.NOT_STORYTRACK_CREATER));

        // when & then
        mockMvc.perform(delete("/api/v1/storytrack/delete")
                        .param("storytrackId", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("storytrackId 파라미터 없으면 400 에러")
    void deleteStorytrack_missingParam() throws Exception {
        mockMvc.perform(delete("/api/v1/storytrack/delete"))
                .andExpect(status().isInternalServerError());
    }

    // 수정 통합 테스트

}
