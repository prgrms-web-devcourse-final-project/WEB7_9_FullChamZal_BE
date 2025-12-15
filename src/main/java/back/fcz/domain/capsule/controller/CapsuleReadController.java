package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/capsule")
public class CapsuleReadController {
    private final CapsuleReadService capsuleReadService;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final CapsuleDashBoardService  capsuleDashBoardService;

    //캡슐 조건 검증 -> 조건 만족 후 읽기
    @PostMapping("/read")
    public ResponseEntity<CapsuleConditionResponseDTO> conditionAndReadCapsule(
            @RequestBody CapsuleConditionRequestDTO capsuleConditionRequestDto
    ) {
        return ResponseEntity.ok(capsuleReadService.conditionAndRead(capsuleConditionRequestDto));
    }


    //회원이 전송한 캡슐의 대시보드 api
    @GetMapping("/send/dashboard/{memberId}")
    public ResponseEntity<ApiResponse<List<CapsuleDashBoardResponse>>> sentCapsuleDash(
            @PathVariable Long memberId
    ) {
        List<CapsuleDashBoardResponse> response = capsuleDashBoardService.readSendCapsuleList(memberId);

        return  ResponseEntity.ok(ApiResponse.success(response));
    }

    //회원이 받은 캡슐의 대시보드 api
    @GetMapping("/receive/dashboard/{memberId}")
    public ResponseEntity<ApiResponse<List<CapsuleDashBoardResponse>>> receivedCapsuleDash(
            @PathVariable Long memberId
    ) {
        List<CapsuleDashBoardResponse> response = capsuleDashBoardService.readReceiveCapsuleList(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
