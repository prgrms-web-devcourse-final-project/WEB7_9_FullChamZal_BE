package back.fcz.domain.storytrack.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.storytrack.dto.request.CreateStorytrackRequest;
import back.fcz.domain.storytrack.dto.request.JoinStorytrackRequest;
import back.fcz.domain.storytrack.dto.request.UpdatePathRequest;
import back.fcz.domain.storytrack.dto.response.*;
import back.fcz.domain.storytrack.service.StorytrackService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.dto.PageResponse;
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

    // 캡슐 대시보드
    private final CapsuleDashBoardService capsuleDashBoardService;

    //삭제
    // 작성자 - 스토리트랙 삭제
    @Operation(summary = "스토리트랙 삭제", description = "스토리트랙 작성자가 스토리트랙을 삭제할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.STORYTRACK_NOT_FOUND,
            ErrorCode.NOT_STORYTRACK_CREATER,
            ErrorCode.PARTICIPANT_EXISTS
    })
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<DeleteStorytrackResponse>> deleteStorytrack(
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
    public ResponseEntity<ApiResponse<UpdatePathResponse>> updatePath(
            @RequestBody UpdatePathRequest request
    ){
         Long loginMember = currentUserContext.getCurrentUser().memberId();

        UpdatePathResponse response = storytrackService.updatePath(request, loginMember);

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    // 생성
    // 스토리트랙 생성
    @Operation(summary = "스토리트랙 생성", description = "공개 상태의 캡슐로 스토리트랙을 생성할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_FOUND,
            ErrorCode.CAPSULE_NOT_PUBLIC
    })
    @PostMapping("/creat")
    public ResponseEntity<ApiResponse<CreateStorytrackResponse>> createStoyrtrack(
            @RequestBody CreateStorytrackRequest request
            ){

        Long loginMember = currentUserContext.getCurrentUser().memberId();

        CreateStorytrackResponse response = storytrackService.createStorytrack(request, loginMember);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 스토리트랙 참여자 생성
    @Operation(summary = "스토리트랙 참여", description = "회원이 스토리트랙에 참여할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.PARTICIPANT_NOT_FOUND,
            ErrorCode.STORYTRACK_NOT_FOUND,
            ErrorCode.STORYTRACK_NOT_PUBLIC
    })
    @PostMapping("/creat/participant")
    public ResponseEntity<ApiResponse<JoinStorytrackResponse>> joinStorytrack(
            @RequestBody JoinStorytrackRequest request
            ){
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        JoinStorytrackResponse response = storytrackService.joinParticipant(request, loginMember);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 조회
    // 전체 스토리트랙 조회
    @Operation(summary = "공개 스토리트랙 목록 조회", description = "PUBLIC 상태인 스토리트랙 목록을 조회할 수 있습니다.")
    @ApiErrorCodeExample({})
    @GetMapping("/List")
    public ResponseEntity<ApiResponse<PageResponse<TotalStorytrackResponse>>> readStorytrackList (
            @RequestParam(defaultValue ="0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        PageResponse<TotalStorytrackResponse> response = storytrackService.readTotalStorytrack(page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 스토리트랙 상세 조회
    @Operation(summary = "스토리트랙 상세 조회", description = "한 스토리트랙에 대한 자세한 내용을 조회할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.STORYTRACK_NOT_FOUND
    })
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StorytrackDashBoardResponse>> dashboard(
            @RequestParam Long storytrackId,
            @RequestParam(defaultValue ="0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        StorytrackDashBoardResponse response = storytrackService.storytrackDashboard(storytrackId, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    // 스토리트랙 경로 조회
    @Operation(summary = "스토리트랙 경로 조회", description = "스토리트랙의 캡슐 순서와 해당 캡슐의 간단한 내용을 조회할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.STORYTRACK_NOT_FOUND
    })
    @GetMapping("/path")
    public ResponseEntity<ApiResponse<StorytrackPathResponse>> storytrackPath(
            @RequestParam Long storytrackId,
            @RequestParam(defaultValue ="0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        StorytrackPathResponse response = storytrackService.storytrackPath(storytrackId, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 생성자 : 스토리트랙 생성 시, 스토리트랙에 사용할 수 있는 캡슐 목록 조회
    @Operation(summary = "내가 만든 장소 기반 공개 캡슐 조회(스토리트랙 생성용 캡슐 조회)",
            description = "장소 기반(LOCATION), 장소+시간 기반(TIME_AND_LOCATION) 공개 캡슐이 조회됩니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE
    })
    @GetMapping("/creater/capsuleList")
    public ResponseEntity<ApiResponse<PageResponse<CapsuleDashBoardResponse>>> findMyLocationCalsuleList (
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        PageResponse<CapsuleDashBoardResponse> response = capsuleDashBoardService.myPublicLocationCapsule(loginMember, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 생성자 : 생성한 스토리트랙 목록 조회
    @Operation(summary = "생성한 스토리트랙 조회", description = "본인이 생성한 스토리트랙을 조회할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE
    })
    @GetMapping("/creater/storytrackList")
    public ResponseEntity<ApiResponse<PageResponse<CreaterStorytrackListResponse>>> createdStorytrackList(
            @RequestParam(defaultValue ="0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        PageResponse<CreaterStorytrackListResponse> response = storytrackService.createdStorytrackList(loginMember, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 참여자 : 참여한 스토리트랙 목록 조회
    @Operation(summary = "참여한 스토리트랙 조회", description = "본인이 참여한 스토리트랙 내역을 조회할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE
    })
    @GetMapping("/participant/joinedList")
    public ResponseEntity<ApiResponse<PageResponse<ParticipantStorytrackListResponse>>> joinedStorytrackList(
            @RequestParam(defaultValue ="0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        PageResponse<ParticipantStorytrackListResponse> response = storytrackService.joinedStorytrackList(loginMember, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 참여자 : 스토리트랙 진행 상세 조회
    @Operation(summary = "스토리트랙 진행 상세 조회", description = "참여한 스토리트랙의 진행상황을 조회할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.PARTICIPANT_NOT_FOUND
    })
    @GetMapping("/participant/progress")
    public ResponseEntity<ApiResponse<ParticipantProgressResponse>> sotyrtrackProgress (
            @RequestParam Long storytrackId
    ){
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        ParticipantProgressResponse response = storytrackService.storytrackProgress(storytrackId, loginMember);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 스토리트랙 캡슐 오픈
    @Operation(summary = "스토리트랙 캡슐 오픈", description = "참여 여부와 현재 단계를 검증, 캡슐 오픈을 진행합니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.PARTICIPANT_NOT_FOUND,
            ErrorCode.STEP_NOT_FOUND,
            ErrorCode.INVALID_STEP_ORDER
    })
    @PostMapping("/participant/capsuleOpen")
    public ResponseEntity<ApiResponse<CapsuleConditionResponseDTO>> storytrackCapsuleOpen (
            @RequestParam Long storytrackId,
            @RequestBody CapsuleConditionRequestDTO request
    ) {
        Long loginMember = currentUserContext.getCurrentUser().memberId();

        // 참여 여부 검증
        storytrackService.validateParticipant(loginMember, storytrackId);

        // 진행 단계 검증

        storytrackService.validateStepAccess(
                loginMember,
                storytrackId,
                request.capsuleId()
        );

        CapsuleConditionResponseDTO response =
                storytrackService.openCapsuleAndUpdateProgress(
                        loginMember,
                        storytrackId,
                        request
                );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
