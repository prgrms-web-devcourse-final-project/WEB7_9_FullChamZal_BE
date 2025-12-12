package back.fcz.domain.unlock.controller;

import back.fcz.domain.unlock.dto.request.NearbyOpenCapsuleRequest;
import back.fcz.domain.unlock.dto.response.NearbyOpenCapsuleResponse;
import back.fcz.domain.unlock.service.NearbyOpenCapsuleService;
import back.fcz.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NearbyOpenCapsuleController {

    private final NearbyOpenCapsuleService nearbyOpenCapsuleService;

    // 사용자 근처 공개 캡슐 리스트 조회
    @GetMapping("/api/v1/capsule/nearby")
    public ResponseEntity<ApiResponse<List<NearbyOpenCapsuleResponse>>> getNearbyOpenCapsules(
            @Valid @ModelAttribute NearbyOpenCapsuleRequest request
    ) {
        List<NearbyOpenCapsuleResponse> response = nearbyOpenCapsuleService.getNearbyOpenCapsules(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
