package back.fcz.domain.storytrack.controller;


import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.storytrack.dto.request.CreateStorytrackRequest;
import back.fcz.domain.storytrack.dto.response.CreateStorytrackResponse;
import back.fcz.domain.storytrack.dto.response.DeleteStorytrackResponse;
import back.fcz.domain.storytrack.dto.response.JoinStorytrackResponse;
import back.fcz.domain.storytrack.service.StorytrackService;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StorytrackControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StorytrackService storytrackService;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private CapsuleDashBoardService capsuleDashBoardService;

    @InjectMocks
    private StorytrackController storytrackController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(storytrackController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void mockLoginUser(Long memberId) {
        InServerMemberResponse user = mock(InServerMemberResponse.class);
        given(user.memberId()).willReturn(memberId);
        given(currentUserContext.getCurrentUser()).willReturn(user);
    }

    @Test
    @DisplayName("스토리트랙 작성자 삭제 성공")
    void deleteStorytrack_success() throws Exception {
        // given
        Long storytrackId = 1L;
        Long loginMemberId = 1L;

        mockLoginUser(loginMemberId);

        DeleteStorytrackResponse response =
                new DeleteStorytrackResponse(storytrackId, "삭제 완료");

        given(storytrackService.deleteStorytrack(loginMemberId, storytrackId))
                .willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/v1/storytrack/delete")
                        .param("storytrackId", storytrackId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.storytrackId").value(1));

        verify(storytrackService).deleteStorytrack(loginMemberId, storytrackId);
    }

    @Test
    @DisplayName("스토리트랙 생성 성공")
    void createStorytrack_success() throws Exception {
        // given
        Long loginMemberId = 1L;
        mockLoginUser(loginMemberId);

        CreateStorytrackResponse response =
                new CreateStorytrackResponse(
                        10L,
                        "title",
                        "desc",
                        "SEQUENTIAL",
                        1,
                        0,
                        3,
                        List.of(100L, 200L, 300L)
                );

        given(storytrackService.createStorytrack(any(), eq(loginMemberId)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/storytrack/creat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "title": "title",
                          "description": "desc",
                          "trackType": "SEQUENTIAL",
                          "isPublic": 1,
                          "price": 0,
                          "capsuleList": [1,2,3],
                          "attachmentId": []
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.totalSteps").value(3));

        verify(storytrackService)
                .createStorytrack(any(CreateStorytrackRequest.class), eq(loginMemberId));
    }

    @Test
    @DisplayName("스토리트랙 참여 성공")
    void joinStorytrack_success() throws Exception {
        // given
        Long loginMemberId = 1L;
        mockLoginUser(loginMemberId);

        JoinStorytrackResponse response =
                new JoinStorytrackResponse(
                        "title",
                        "desc",
                        "SEQUENTIAL",
                        0,
                        5,
                        "creator",
                        0,
                        0,
                        LocalDateTime.now()
                );

        given(storytrackService.joinParticipant(any(), eq(loginMemberId)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/storytrack/creat/participant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "storytrackId": 10
                        }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.completedSteps").value(0));
    }

    @Test
    @DisplayName("스토리트랙 캡슐 오픈 성공")
    void openCapsule_success() throws Exception {
        Long loginMemberId = 1L;
        Long storytrackId = 10L;

        mockLoginUser(loginMemberId);

        CapsuleConditionResponseDTO response = mock(CapsuleConditionResponseDTO.class);

        willDoNothing().given(storytrackService)
                .validateParticipant(loginMemberId, storytrackId);

        willDoNothing().given(storytrackService)
                .validateStepAccess(eq(loginMemberId), eq(storytrackId), anyLong());

        given(storytrackService.openCapsuleAndUpdateProgress(
                eq(loginMemberId),
                eq(storytrackId),
                any()
        )).willReturn(response);

        mockMvc.perform(post("/api/v1/storytrack/participant/capsuleOpen")
                        .param("storytrackId", storytrackId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "capsuleId": 20,
                          "unlockAt": "2025-12-20T23:00:10"
                        }
                        """))
                .andExpect(status().isOk());

        verify(storytrackService).validateParticipant(loginMemberId, storytrackId);
        verify(storytrackService).validateStepAccess(loginMemberId, storytrackId, 20L);
    }

    @Test
    @DisplayName("스토리트랙 생성용 캡슐 목록 조회 성공")
    void findMyLocationCapsuleList_success() throws Exception {
        Long memberId = 1L;
        mockLoginUser(memberId);

        CapsuleDashBoardResponse dto =
                new CapsuleDashBoardResponse(
                        1L,
                        "red",
                        "white",
                        "receiver",
                        "sender",
                        "title",
                        "content",
                        LocalDateTime.now(),
                        false,
                        "LOCATION",
                        null,
                        "서울",
                        37.1,
                        127.1
                );

        PageResponse<CapsuleDashBoardResponse> pageResponse =
                new PageResponse<>(new PageImpl<>(List.of(dto)));

        given(capsuleDashBoardService.myPublicLocationCapsule(memberId, 0, 10))
                .willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/storytrack/creater/capsuleList")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].capsuleId").value(1))
                .andExpect(jsonPath("$.data.content[0].unlockType").value("LOCATION"));
    }
}
