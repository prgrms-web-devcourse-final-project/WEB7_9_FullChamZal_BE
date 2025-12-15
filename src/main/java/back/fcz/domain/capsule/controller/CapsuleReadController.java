package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/capsule")
@Tag(
        name = "캡슐 API",
        description = "캡슐 조회 관련 API"
)
public class CapsuleReadController {
    private final CapsuleReadService capsuleReadService;
    private final CapsuleDashBoardService  capsuleDashBoardService;


    //캡슐 조건 검증 -> 조건 만족 후 읽기
    @PostMapping("/read")
    public ResponseEntity<CapsuleConditionResponseDTO> conditionAndReadCapsule(
            @RequestBody CapsuleConditionRequestDTO capsuleConditionRequestDto
    ) {
        System.out.println("캡슐 조건 검증 컨트롤러 진입");
        return ResponseEntity.ok(capsuleReadService.conditionAndRead(capsuleConditionRequestDto));
    }


    @Operation(summary = "전송한 캡슐 조회", description = "사용자가 전송한 캡슐들을 조회합니다. 이때, 삭제되지 않은 캡슐만 조회가 가능합니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_RECIPIENT_NOT_FOUND
    })
    @GetMapping("/send/dashboard")
    public ResponseEntity<ApiResponse<List<CapsuleDashBoardResponse>>> sentCapsuleDash(
            @AuthenticationPrincipal Long memberId
    ) {
        List<CapsuleDashBoardResponse> response = capsuleDashBoardService.readSendCapsuleList(memberId);

        return  ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "수신받은 캡슐 조회", description = "사용자가 수신받은 캡슐들을 조회합니다. 이때, 삭제되지 않은 캡슐만 조회가 가능합니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND
    })
    @GetMapping("/receive/dashboard")
    public ResponseEntity<ApiResponse<List<CapsuleDashBoardResponse>>> receivedCapsuleDash(
            @AuthenticationPrincipal Long memberId
    ) {
        List<CapsuleDashBoardResponse> response = capsuleDashBoardService.readReceiveCapsuleList(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
