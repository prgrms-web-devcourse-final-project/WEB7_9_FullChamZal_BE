package back.fcz.domain.storytrack.controller;

import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.storytrack.dto.request.UpdatePathRequest;
import back.fcz.domain.storytrack.dto.response.DeleteParticipantResponse;
import back.fcz.domain.storytrack.dto.response.DeleteStorytrackResponse;
import back.fcz.domain.storytrack.dto.response.UpdatePathResponse;
import back.fcz.domain.storytrack.service.StorytrackService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "스토리트랙 API", description = "스토리트랙 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storytrack")
public class StorytrackController {

    // 로그인 회원 확인
    private final CurrentUserContext currentUserContext;

    // 스토리트랙 서비스
    private final StorytrackService storytrackService;

    //삭제
    // 작성자 - 스토리트랙 삭제
    @Operation(summary = "스토리트랙 삭제", description = "스토리트랙 작성자가 스토리트랙을 삭제할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.STORYTRACK_NOT_FOUND,
            ErrorCode.NOT_STORYTRACK_CREATER,
            ErrorCode.PARTICIPANT_EXISTS
    })
    @DeleteMapping("/delete")
    public ResponseEntity
            <ApiResponse<DeleteStorytrackResponse>> deleteStorytrack(
            @RequestParam Long storytrackId
    ){
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        DeleteStorytrackResponse response = storytrackService.deleteStorytrack(loginMember, storytrackId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 참여자 - 참여 스토리트랙 삭제(참여종료)
    @Operation(summary = "참여 스토리트랙 삭제", description = "스토리트랙 참여자가 참여한 스토리트랙을 삭제할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.PARTICIPANT_NOT_FOUND
    })
    @DeleteMapping("/delete/participant")
    public ResponseEntity<ApiResponse<DeleteParticipantResponse>> deleteParticipant(
            @RequestParam Long storytrackId
    ){
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        DeleteParticipantResponse response = storytrackService.deleteParticipant(loginMember, storytrackId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 수정
    // 작성자 - 스토리트랙 경로 수정
    @Operation(summary = "스토리트랙 경로 수정", description = "스토리트랙 작성자가 스토리트랙 경로를 수정할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.STORYTRACK_PAHT_NOT_FOUND,
            ErrorCode.NOT_STORYTRACK_CREATER,
            ErrorCode.CAPSULE_NOT_FOUND
    })
    @PutMapping("/update")
    public ResponseEntity
            <ApiResponse<UpdatePathResponse>> updatePath(
            @RequestParam Long storytrackStepId,
            @RequestBody UpdatePathRequest request
    ){
         Long loginMember = currentUserContext.getCurrentUser().memberId();

        UpdatePathResponse response = storytrackService.updatePath(request, storytrackStepId, loginMember);

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    // 생성
    // 스토리트랙 생성

    // 스토리트랙 참여자 생성

    // 조회
    // 전체 스토리트랙 조회

    // 스토리트랙 상세 조회

    // 스토리트랙 경로 조회

    // 생성자 : 생성한 스토리트랙 목록 조회

    // 참여자 : 참여한 스토리트랙 목록 조회

    // 참여자 : 스토리트랙 진행 상세 조회

    // 스토리트랙 캡슐 오픈
}
