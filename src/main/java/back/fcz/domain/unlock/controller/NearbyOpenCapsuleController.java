package back.fcz.domain.unlock.controller;

import back.fcz.domain.unlock.dto.request.NearbyOpenCapsuleRequest;
import back.fcz.domain.unlock.dto.response.NearbyOpenCapsuleResponse;
import back.fcz.domain.unlock.service.NearbyOpenCapsuleService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "해제 조건 API",
        description = "해제 조건 관련 API"
)
public class NearbyOpenCapsuleController {

    private final NearbyOpenCapsuleService nearbyOpenCapsuleService;

    @Operation(summary = "근처 공개 캡슐 리스트 조회", description = "사용자 근처의 공개 캡슐 리스트를 조회합니다. 기본 반경 값은 1km입니다.")
    @ApiErrorCodeExample({
            ErrorCode.INVALID_RADIUS
    })
    @GetMapping("/api/v1/capsule/nearby")
    public ResponseEntity<ApiResponse<List<NearbyOpenCapsuleResponse>>> getNearbyOpenCapsules(
            @Valid @ModelAttribute NearbyOpenCapsuleRequest request
    ) {
        List<NearbyOpenCapsuleResponse> response = nearbyOpenCapsuleService.getNearbyOpenCapsules(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
