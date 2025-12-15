package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleReadRequestDto;
import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.DTO.response.CapsuleReadResponseDto;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
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
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final CapsuleDashBoardService  capsuleDashBoardService;

    //캡슐 조건 검증 -> 조건 만족 후 읽기
    @GetMapping("/{capsuleId}")
    public ResponseEntity<CapsuleReadResponseDto> conditionAndReadCapsule(
            @RequestBody CapsuleReadRequestDto capsuleReadRequestDto
    ) {
        Capsule resultCapsule = capsuleReadService.getCapsule(capsuleReadRequestDto.capsuleId());
        //isProtected확인(0이면 수신자가 비회원  /  1이면 수신자가 회원)
        if(resultCapsule.getIsProtected()==1){  //isProtected=1 -> 회원

            if(capsuleReadService.phoneNumberVerification(resultCapsule, capsuleReadRequestDto.phoneNumber(),
                    capsuleReadRequestDto.unlockAt(), capsuleReadRequestDto.locationLat(), capsuleReadRequestDto.locationLng()))
            {
                CapsuleRecipient capsuleRecipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(resultCapsule.getCapsuleId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

                //응답 Dto생성
                CapsuleReadResponseDto capsuleReadResponseDto = new CapsuleReadResponseDto(
                        resultCapsule.getCapsuleId(),
                        resultCapsule.getCapsuleColor(),
                        resultCapsule.getCapsulePackingColor(),
                        capsuleRecipient.getRecipientName(),
                        resultCapsule.getNickname(),
                        resultCapsule.getTitle(),
                        resultCapsule.getContent(),
                        resultCapsule.getCreatedAt(),
                        resultCapsule.getCurrentViewCount() > 0,
                        resultCapsule.getUnlockType(),
                        resultCapsule.getUpdatedAt(),
                        resultCapsule.getLocationName(),
                        resultCapsule.getLocationLat(),
                        resultCapsule.getLocationLng()
                );
                return ResponseEntity.ok(capsuleReadResponseDto);
            }
        } else{ //isProtected()가 0인경우 -> 비회원

            if(capsuleReadService.urlAndPasswordVerification(resultCapsule, capsuleReadRequestDto.password(), resultCapsule.getCapPassword(),
                    capsuleReadRequestDto.unlockAt(), capsuleReadRequestDto.locationLat(), capsuleReadRequestDto.locationLng()))
            {
                CapsuleRecipient capsuleRecipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(resultCapsule.getCapsuleId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

                //응답 Dto생성
                CapsuleReadResponseDto capsuleReadResponseDto = new CapsuleReadResponseDto(
                        resultCapsule.getCapsuleId(),
                        resultCapsule.getCapsuleColor(),
                        resultCapsule.getCapsulePackingColor(),
                        capsuleRecipient.getRecipientName(),
                        resultCapsule.getNickname(),
                        resultCapsule.getTitle(),
                        resultCapsule.getContent(),
                        resultCapsule.getCreatedAt(),
                        resultCapsule.getCurrentViewCount() > 0,
                        resultCapsule.getUnlockType(),
                        resultCapsule.getUpdatedAt(),
                        resultCapsule.getLocationName(),
                        resultCapsule.getLocationLat(),
                        resultCapsule.getLocationLng());
                return ResponseEntity.ok(capsuleReadResponseDto);
            }
            
        }

        throw new BusinessException(ErrorCode.CAPSULE_CONDITION_ERROR);
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


    //캡슐 저장버튼을 눌렀을때 호출할 api
    //수신자 테이블에 저장하는게 맞음


    //사용자가 열람한 적이 있는 캡슐이라면 조건없이 읽기(이건 그냥 읽기)



}
