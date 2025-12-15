package back.fcz.domain.capsule.controller;

<<<<<<< HEAD
import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleSendDashBoardRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleSendDashBoardResponseDTO;
=======
import back.fcz.domain.capsule.DTO.request.CapsuleReadRequestDto;
import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.DTO.response.CapsuleReadResponseDto;
>>>>>>> dev
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
<<<<<<< HEAD
import org.springframework.web.bind.annotation.*;
=======
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
>>>>>>> dev

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
