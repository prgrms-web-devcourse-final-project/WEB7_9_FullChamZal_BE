package back.fcz.domain.capsule.controller;


import back.fcz.domain.capsule.DTO.request.CapsuleAttachmentUploadRequest;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.service.AttachmentService;
import back.fcz.domain.capsule.service.CapsuleAttachmentPresignService;
import back.fcz.domain.capsule.service.CapsuleAttachmentServerUploadService;
import back.fcz.domain.member.service.CurrentUserContext;
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
@RequestMapping("/api/v1/capsule/upload")
@Tag(name = "캡슐 파일 업로드 API", description = "캡슐 파일 업로드 관련 API")
public class CapsuleAttachmentController {

    private final CapsuleAttachmentPresignService capsuleAttachmentPresignService;
    private final CapsuleAttachmentServerUploadService capsuleAttachmentServerUploadService;
    private final CurrentUserContext currentUserContext;
    private final AttachmentService attachmentService;

    @Operation(summary = "파일 서버 업로드 방식", description = "클라이언트에서 파일을 보내면 서버에서 s3에 업로드합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CapsuleAttachmentUploadResponse>> uploadByServer(
            @RequestPart("file") MultipartFile file
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        CapsuleAttachmentUploadResponse response = capsuleAttachmentServerUploadService.uploadTemp(memberId, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "PresignedUrl 업로드 방식", description = "클라이언트에서 URL을 받아서 s3에 업로드합니다.")
    @PostMapping(value = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CapsuleAttachmentUploadResponse>> uploadByPresignUrl(
            @Valid @RequestBody CapsuleAttachmentUploadRequest request
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        CapsuleAttachmentUploadResponse response = capsuleAttachmentPresignService.presignedUpload(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "임시 파일 삭제", description = "업로드한 임시 파일을 삭제합니다.")
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteTempFile(
            @PathVariable Long attachmentId
    ){
        Long memberId = currentUserContext.getCurrentMemberId();
        attachmentService.deleteTemp(memberId, attachmentId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
