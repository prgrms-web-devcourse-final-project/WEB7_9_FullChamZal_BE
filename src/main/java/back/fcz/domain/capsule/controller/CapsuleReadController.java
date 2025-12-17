package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
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
        description = "캡슐 관련 API"
)
public class CapsuleReadController {
    private final CapsuleReadService capsuleReadService;
    private final CapsuleDashBoardService  capsuleDashBoardService;


    //캡슐 조건 검증 -> 조건 만족 후 읽기
    @Operation(summary = "요청 캡슐 검증 및 조회",
            description = "사용자가 받은 캡슐의 내용을 조건에 맞으면 보여줍니다. "
                    +  "이미 조회한 캡슐이라면 조건 검증을 생략합니다.")
    @ApiErrorCodeExample({
            ErrorCode.NOT_OPENED_CAPSULE,
            ErrorCode.CAPSULE_NOT_RECEIVER,
            ErrorCode.CAPSULE_NOT_FOUND,
            ErrorCode.CAPSULE_PASSWORD_NOT_MATCH,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.RECIPIENT_NOT_FOUND
    })
    @PostMapping("/read")
    public ResponseEntity<ApiResponse<CapsuleConditionResponseDTO>> conditionAndReadCapsule(
            @RequestBody CapsuleConditionRequestDTO capsuleConditionRequestDto
    ) {
        System.out.println("캡슐 조건 검증 컨트롤러 진입");
        return ResponseEntity.ok(ApiResponse.success(capsuleReadService.conditionAndRead(capsuleConditionRequestDto)));
    }

    //캡슐 저장 버튼
    @Operation(summary = "",
            description = "url+비밀번호 읽기를 성공했을때 호출되는 api입니다.")
    @ApiErrorCodeExample({

    })
    @PostMapping("/save")
    public ResponseEntity<Void> save(

    ){
        //로그인 상태가 아닐경우 회원가입이나 로그인 창으로 보내기

        //로그인 상태라면 개인 캡슐 수신자 정보 생성(현재 로그인 중인 회원의 데이터 기록)
        return ResponseEntity.ok().build();
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
