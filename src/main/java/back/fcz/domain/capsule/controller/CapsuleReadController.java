package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleSaveButtonRequest;
import back.fcz.domain.capsule.DTO.response.*;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.domain.capsule.service.CapsuleSaveButtonService;
import back.fcz.domain.sanction.util.RequestInfoExtractor;
import back.fcz.global.aop.AllowDuplicateRequest;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/capsule")
@Tag(
        name = "캡슐 API",
        description = "캡슐 관련 API"
)
public class CapsuleReadController {
    private final CapsuleReadService capsuleReadService;
    private final CapsuleDashBoardService  capsuleDashBoardService;
    private final CapsuleSaveButtonService  capsuleSaveButtonService;

    //캡슐 읽기
    @Operation(summary = "수신자 캡슐 조회",
            description = "사용자가 보낸 캡슐의 내용을 조건 없이 보여줍니다. "
    )
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_FOUND,
            ErrorCode.NOT_SELF_CAPSULE
    })
    @GetMapping("/readSendCapsule")
    public ResponseEntity<ApiResponse<CapsuleConditionResponseDTO>> readSendCapsule(
            @RequestParam  Long capsuleId
    ) {
        return ResponseEntity.ok(ApiResponse.success(capsuleReadService.capsuleRead(capsuleId)));
    }

    //캡슐의 비밀번호 존재 여부
    @Operation(summary = "캡슐의 비밀번호 존재 여부",
            description = "캡슐 UUID를 받으면 해당 캡슐의 ID와 비밀번호 설정 여부를 알려줍니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_FOUND
    })
    @GetMapping("/readCapsule")
    public ResponseEntity<ApiResponse<CapsuleReadResponse>> readCapsule(
            @RequestParam String uuid
    ){
        return ResponseEntity.ok(ApiResponse.success(capsuleReadService.existedPassword(uuid)));
    }

    //캡슐 조건 검증 -> 조건 만족 후 읽기
    @Operation(summary = "요청 캡슐 검증 및 조회",
            description = "사용자가 받은 캡슐의 내용을 조건에 맞으면 보여줍니다. "
                    +  "이미 조회한 캡슐이라면 조건 검증을 생략합니다.")
    @ApiErrorCodeExample({
            ErrorCode.NOT_OPENED_CAPSULE,
            ErrorCode.CAPSULE_NOT_RECEIVER,
            ErrorCode.CAPSULE_NOT_FOUND,
            ErrorCode.CAPSULE_PASSWORD_NOT_MATCH,
            ErrorCode.CAPSULE_PASSWORD_REQUIRED,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.RECIPIENT_NOT_FOUND,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.LOCATION_REQUIRED
    })
    @PostMapping("/read")
    @AllowDuplicateRequest
    public ResponseEntity<ApiResponse<CapsuleConditionResponseDTO>> conditionAndReadCapsule(
            @RequestBody CapsuleConditionRequestDTO capsuleConditionRequestDto,
            HttpServletRequest request
    ) {
        String ipAddress = RequestInfoExtractor.extractIp(request);
        String userAgent = RequestInfoExtractor.extractUserAgent(request);

        LocalDateTime serverTime = LocalDateTime.now(ZoneOffset.UTC);

        CapsuleConditionRequestDTO plusDto = new CapsuleConditionRequestDTO(
                capsuleConditionRequestDto.capsuleId(),
                capsuleConditionRequestDto.unlockAt(),
                capsuleConditionRequestDto.locationLat(),
                capsuleConditionRequestDto.locationLng(),
                capsuleConditionRequestDto.password(),
                userAgent,
                ipAddress,
                serverTime
        );
        return ResponseEntity.ok(ApiResponse.success(capsuleReadService.conditionAndRead(plusDto)));
    }

    //캡슐 저장 버튼(비회원이 CapsuleRecipient기록을 남길때 호출됩니다.)
    @Operation(summary = "url + 비밀번호로 생성된 캡슐 저장",
            description = "url + 비밀번호로 생성된 캡슐을 저장하는 API입니다. 저장 시 isProtected 값이 1로 변경됩니다.")
    @ApiErrorCodeExample({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.CAPSULE_NOT_FOUND,
            ErrorCode.CAPSULE_ALREADY_SAVED,
            ErrorCode.PUBLIC_CAPSULE_CANNOT_BE_SAVED,
            ErrorCode.CAPSULE_OPEN_LOG_NOT_FOUND
    })
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<CapsuleSaveButtonResponse>> save(
            @RequestBody CapsuleSaveButtonRequest capsuleSaveButtonRequest){
        return ResponseEntity.ok(ApiResponse.success(capsuleSaveButtonService.saveRecipient(capsuleSaveButtonRequest)));
    }


    @Operation(summary = "전송한 캡슐 조회", description = "사용자가 전송한 캡슐들을 조회합니다. 이때, 삭제되지 않은 캡슐만 조회가 가능합니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_RECIPIENT_NOT_FOUND
    })
    @GetMapping("/send/dashboard")
    public ResponseEntity<ApiResponse<Page<CapsuleDashBoardResponse>>> sentCapsuleDash(
            @AuthenticationPrincipal Long memberId,
            @ParameterObject @PageableDefault(size =10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CapsuleDashBoardResponse> response = capsuleDashBoardService.readSendCapsuleList(memberId, pageable);

        return  ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "수신받은 캡슐 조회", description = "사용자가 수신받은 캡슐들을 조회합니다. 이때, 삭제되지 않은 캡슐만 조회가 가능합니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND
    })
    @GetMapping("/receive/dashboard")
    public ResponseEntity<ApiResponse<Page<CapsuleDashBoardResponse>>> receivedCapsuleDash(
            @AuthenticationPrincipal Long memberId,
            @ParameterObject @PageableDefault(size =10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CapsuleDashBoardResponse> response = capsuleDashBoardService.readReceiveCapsuleList(memberId, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "연간 송수신 캡슐 조회",
            description = "사용자가 연간 송수신 한 캡슐들의 수를 조회합니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND
    })
    @GetMapping("/showYearlyCapsule")
    public ResponseEntity<ApiResponse<YearlyCapsuleResponse>> showYearlyCapsule(
            @AuthenticationPrincipal Long memberId,
            @RequestParam int year
    ) {
        return  ResponseEntity.ok(ApiResponse.success(capsuleDashBoardService.getYearlyCapsule(memberId, year)));
    }

    @Operation(summary = "오늘 해제 될 캡슐 조회",
            description = "사용자가 받은 캡슐 중 오늘 해제될 캡슐들을 반환합니다.")
    @ApiErrorCodeExample({

    })
    @GetMapping("/dailyUnlockedCapsule")
    public ResponseEntity<ApiResponse<DailyUnlockedCapsuleResponse>> dailyUnlockedCapsule(
            @AuthenticationPrincipal Long memberId
    ) {
        DailyUnlockedCapsuleResponse response= capsuleDashBoardService.dailyUnlockedCapsule(memberId);
        return  ResponseEntity.ok(ApiResponse.success(response));
    }


}
