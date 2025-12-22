package back.fcz.domain.capsule.controller;


import back.fcz.domain.capsule.DTO.request.CapsuleLikeRequest;
import back.fcz.domain.capsule.DTO.response.CapsuleLikeResponse;
import back.fcz.domain.capsule.service.CapsuleLikeService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/capsule")
@Tag(
        name = "캡슐 좋아요 API",
        description = "캡슐 좋아요 관련 API"
)
public class CapsuleLikeController {
    private final CapsuleLikeService capsuleLikeService;

    @Operation(summary = "캡슐 좋아요 수 읽기",
            description = "해당 캡슐의 좋아요 수를 읽습니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_FOUND
    })
    @GetMapping("/readLike")
    public ResponseEntity<ApiResponse<CapsuleLikeResponse>> readLike(
            @RequestParam Long capsuleId
    ){
        return ResponseEntity.ok(ApiResponse.success(capsuleLikeService.readLike(capsuleId)));
    }


    @Operation(summary = "캡슐 좋아요 수 증가",
            description = "사용자가 좋아요 버튼을 누르면 좋아요 수가 증가합니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.DUPLICATE_LIKE_REQUEST,
            ErrorCode.SELF_LIKE_NOT_ALLOWED,
            ErrorCode.CAPSULE_NOT_FOUND,
            ErrorCode.MEMBER_NOT_FOUND
    })
    @PostMapping("/likeUp")
    public ResponseEntity<ApiResponse<CapsuleLikeResponse>> likeUp(
            @RequestBody CapsuleLikeRequest capsuleLikeRequest
    ){
        return ResponseEntity.ok(ApiResponse.success(capsuleLikeService.likeUp(capsuleLikeRequest.capsuleId())));
    }

    @Operation(summary = "캡슐 좋아요 수 감소",
            description = "사용자가 좋아요 버튼을 누르면 좋아요 수가 감소합니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.LIKE_DECREASED_FAIL,
            ErrorCode.CAPSULE_NOT_FOUND
    })
    @PostMapping("/likeDown")
    public ResponseEntity<ApiResponse<CapsuleLikeResponse>> likeDown(
            @RequestBody CapsuleLikeRequest capsuleLikeRequest
    ){
        return ResponseEntity.ok(ApiResponse.success(capsuleLikeService.likeDown(capsuleLikeRequest.capsuleId())));
    }
}
