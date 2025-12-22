package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleUpdateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleCreateResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDeleteResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleUpdateResponseDTO;
import back.fcz.domain.capsule.DTO.response.SecretCapsuleCreateResponseDTO;
import back.fcz.domain.capsule.service.CapsuleCreateService;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "캡슐 API", description = "캡슐 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/capsule")
public class CapsuleCreateController {

    private final CapsuleCreateService capsuleCreateService;
    private final CurrentUserContext currentUserContext;

    // 캡슐 생성
    // 공개 캡슐
    @Operation(summary = "공개 캡슐 생성", description = "공개 캡슐을 생성합니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_CREATE,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.NICKNAME_REQUIRED,
            ErrorCode.PHONENUMBER_REQUIRED
    })
    @PostMapping("/create/public")
    public ResponseEntity<ApiResponse<CapsuleCreateResponseDTO>> createPublicCapsule(
            @RequestBody CapsuleCreateRequestDTO requestDTO
            ){
        CapsuleCreateResponseDTO response = capsuleCreateService.publicCapsuleCreate(requestDTO);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 비공개 캡슐
    @Operation(summary = "비공개 캡슐 생성", description = "전화번호와 비밀번호의 유무에 따라 url+비밀번호 방식, 전화 번호 입력 방식을 사용합니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_CREATE,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.RECEIVERNICKNAME_IS_REQUIRED,
            ErrorCode.NICKNAME_REQUIRED,
            ErrorCode.PHONENUMBER_REQUIRED
    })
    @PostMapping("/create/private")
    public ResponseEntity<ApiResponse<SecretCapsuleCreateResponseDTO>> createPrivateCapsule(
            @RequestBody SecretCapsuleCreateRequestDTO requestDTO
    ){
        SecretCapsuleCreateResponseDTO response = capsuleCreateService.createPrivateCapsule(requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 비공개 캡슐 - 나에게 보내는 캡슐
    @Operation(summary = "비공개 캡슐 생성", description = "나에게 보내는 캡슐 생성입니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.RECEIVERNICKNAME_IS_REQUIRED,
            ErrorCode.NICKNAME_REQUIRED,
            ErrorCode.PHONENUMBER_REQUIRED
    })
    @PostMapping("/create/me")
    public ResponseEntity
            <ApiResponse<SecretCapsuleCreateResponseDTO>> createToMeCapsule(
            @RequestBody SecretCapsuleCreateRequestDTO requestDTO
    ){
        InServerMemberResponse currentUser = currentUserContext.getCurrentUser();

        SecretCapsuleCreateResponseDTO response = capsuleCreateService.capsuleToMe(requestDTO, currentUser.phoneNumber(), currentUser.phoneHash());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 캡슐 수정
    @Operation(summary = "캡슐 수정API", description = "캡슐이 열람 되었는지 확인 후 수정 가능하도록 되어있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_UPDATE,
            ErrorCode.CAPSULE_NOT_FOUND
    })
    @PutMapping("/update")
    public ResponseEntity
            <ApiResponse<CapsuleUpdateResponseDTO>> updateCapsule(
            @RequestParam Long capsuleId,
            @RequestBody CapsuleUpdateRequestDTO requestDTO
    ){
        CapsuleUpdateResponseDTO response = capsuleCreateService.updateCapsule(capsuleId, requestDTO);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 캡슐 삭제 - 발신자 삭제
    @Operation(summary = "캡슐 삭제 API", description = "캡슐 발신자가 본인이 생성한 캡슐을 삭제할 수 있도록 하는 API입니다.")
    @ApiErrorCodeExample({ ErrorCode.CAPSULE_NOT_FOUND })
    @DeleteMapping("/delete/sender")
    public ResponseEntity<ApiResponse<CapsuleDeleteResponseDTO>> senderDeleteCapsule(
            @RequestParam Long capsuleId
    ){
        InServerMemberResponse loginUser = currentUserContext.getCurrentUser();

        CapsuleDeleteResponseDTO response = capsuleCreateService.senderDelete(capsuleId, loginUser.memberId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 캡슐 삭제 - 수신자
    @Operation(summary = "캡슐 삭제 API", description = "캡슐 수신자가 본인이 받은 캡슐을 저장 목록에서 삭제할 수 있도록 하는 API입니다.")
    @ApiErrorCodeExample({ ErrorCode.CAPSULE_NOT_FOUND })
    @DeleteMapping("/delete/reciver")
    public ResponseEntity
            <ApiResponse<CapsuleDeleteResponseDTO>> reciverDeleteCapsule(
            @RequestParam Long capsuleId
    ){
        // 현재 로그인 한 사용자 id
        InServerMemberResponse loginUser = currentUserContext.getCurrentUser();

        CapsuleDeleteResponseDTO response = capsuleCreateService.receiverDelete(capsuleId, loginUser.phoneHash());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}