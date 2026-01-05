package back.fcz.domain.storytrack.controller;

import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.storytrack.dto.request.StorytrackAttachmentUploadRequest;
import back.fcz.domain.storytrack.dto.response.StorytrackAttachmentStatusResponse;
import back.fcz.domain.storytrack.dto.response.StorytrackAttachmentUploadResponse;
import back.fcz.domain.storytrack.service.StorytrackAttachmentPresignService;
import back.fcz.domain.storytrack.service.StorytrackAttachmentService;
import back.fcz.domain.storytrack.service.StorytrackAttachmentUploadService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storytrack/upload")
@Tag(name = "스토리트랙 썸네일 업로드 API", description = "스토리트랙 썸네일 업로드 관련 API")
public class StorytrackAttachmentController {

    private final StorytrackAttachmentUploadService storytrackAttachmentUploadService;
    private final StorytrackAttachmentService storytrackAttachmentService;
    private final StorytrackAttachmentPresignService  storytrackAttachmentPresignService;
    private final CurrentUserContext currentUserContext;


    @Operation(summary = "파일 서버 업로드 방식", description = "클라이언트에서 파일을 보내면 서버에서 s3에 업로드합니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_FILE_UPLOAD_FAILED,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_INVALID
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StorytrackAttachmentUploadResponse>> uploadByServer(
            @RequestPart("file") MultipartFile file
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        StorytrackAttachmentUploadResponse response = storytrackAttachmentUploadService.uploadTemp(memberId, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "PresignedUrl 업로드 방식", description = "클라이언트에서 URL을 받아서 s3에 업로드합니다.")
    @ApiErrorCodeExample({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_INVALID
    })
    @PostMapping(value = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<StorytrackAttachmentUploadResponse>> uploadByPresignUrl(
            @Valid @RequestBody StorytrackAttachmentUploadRequest request
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        StorytrackAttachmentUploadResponse response = storytrackAttachmentPresignService.presignedUpload(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @PostMapping("/presign/{attachmentId}")
    @ApiErrorCodeExample({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_INVALID,
            ErrorCode.STORYTRACK_FILE_NOT_FOUND,
            ErrorCode.STORYTRACK_FILE_ATTACH_FORBIDDEN,
            ErrorCode.STORYTRACK_FILE_ATTACH_INVALID_STATUS,
            ErrorCode.STORYTRACK_FILE_UPLOAD_NOT_FINISHED,
            ErrorCode.STORYTRACK_FILE_UPLOAD_SIZE_MISMATCH,
            ErrorCode.STORYTRACK_FILE_UPLOAD_TYPE_MISMATCH,
            ErrorCode.CAPSULE_CONTENT_BLOCKED,
            ErrorCode.OPENAI_MODERATION_FAILED
    })
    public ResponseEntity<ApiResponse<Void>> completeUpload(
            @PathVariable Long attachmentId
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        storytrackAttachmentPresignService.completeUpload(memberId, attachmentId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("/presign/{attachmentId}")
    @ApiErrorCodeExample({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_INVALID,
            ErrorCode.STORYTRACK_FILE_NOT_FOUND,
            ErrorCode.STORYTRACK_FILE_ATTACH_FORBIDDEN
    })
    public ResponseEntity<ApiResponse<StorytrackAttachmentStatusResponse>> getStatus(
            @PathVariable Long attachmentId
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        StorytrackAttachmentStatusResponse response = storytrackAttachmentPresignService.getStatus(memberId, attachmentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "임시 파일 삭제", description = "업로드한 임시 파일을 삭제합니다.")
    @ApiErrorCodeExample({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_INVALID,
            ErrorCode.STORYTRACK_FILE_DELETE_FORBIDDEN
    })
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteTempFile(
            @PathVariable Long attachmentId
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        storytrackAttachmentService.deleteTemp(memberId, attachmentId);
        return ResponseEntity.ok(ApiResponse.success());
    }

}
