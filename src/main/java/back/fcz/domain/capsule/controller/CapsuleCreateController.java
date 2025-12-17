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
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
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
            ErrorCode.MEMBER_NOT_FOUND
    })
    @PostMapping("/create/public")
    public ResponseEntity<CapsuleCreateResponseDTO> createPublicCapsule(
            @RequestBody CapsuleCreateRequestDTO requestDTO
            ){
        return ResponseEntity.ok(capsuleCreateService.publicCapsuleCreate(requestDTO));
    }

    // 비공개 캡슐
    @Operation(summary = "비공개 캡슐 생성", description = "전화번호와 비밀번호의 유무에 따라 url+비밀번호 방식, 전화 번호 입력 방식을 사용합니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_CREATE,
            ErrorCode.MEMBER_NOT_FOUND
    })
    @PostMapping("/create/private")
    public ResponseEntity<SecretCapsuleCreateResponseDTO> createPrivateCapsule(
            @RequestParam(required = false) String phoneNum,
            @RequestParam(required = false) String capsulePassword,
            @RequestBody SecretCapsuleCreateRequestDTO requestDTO
    ){
        if(phoneNum == null){ // url + 비밀번호 방식
            return ResponseEntity.ok(capsuleCreateService.privateCapsulePassword(requestDTO, capsulePassword));
        }

        if(capsulePassword == null){ // 전화 번호 입력 방식
            return ResponseEntity.ok(capsuleCreateService.privateCapsulePhone(requestDTO, phoneNum));
        }

        throw new BusinessException(ErrorCode.CAPSULE_NOT_CREATE);
    }

    // 비공개 캡슐 - 나에게 보내는 캡슐
    @Operation(summary = "비공개 캡슐 생성", description = "나에게 보내는 캡슐 생성입니다.")
    @ApiErrorCodeExample({ ErrorCode.MEMBER_NOT_FOUND })
    @PostMapping("/create/me")
    public ResponseEntity<SecretCapsuleCreateResponseDTO> createToMeCapsule(
            @RequestParam("phone") String receiveTel,
            @RequestBody SecretCapsuleCreateRequestDTO requestDTO
    ){

        return ResponseEntity.ok(capsuleCreateService.capsuleToMe(requestDTO, receiveTel));
    }

    // 캡슐 수정
    @Operation(summary = "캡슐 수정API", description = "캡슐이 열람 되었는지 확인 후 수정 가능하도록 되어있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_UPDATE,
            ErrorCode.CAPSULE_NOT_FOUND
    })
    @PutMapping("/update")
    public ResponseEntity<CapsuleUpdateResponseDTO> updateCapsule(
            @RequestParam Long capsuleId,
            @RequestBody CapsuleUpdateRequestDTO requestDTO
    ){
        return ResponseEntity.ok(capsuleCreateService.updateCapsule(capsuleId, requestDTO));
    }

    // 캡슐 삭제 - 발신자 삭제
    @Operation(summary = "캡슐 삭제 API", description = "캡슐 발신자가 본인이 생성한 캡슐을 삭제할 수 있도록 하는 API입니다.")
    @ApiErrorCodeExample({ ErrorCode.CAPSULE_NOT_FOUND })
    @DeleteMapping("/delete/sender")
    public ResponseEntity<CapsuleDeleteResponseDTO> senderDeleteCapsule(
            @RequestParam Long capsuleId
    ){
        InServerMemberResponse loginUser = currentUserContext.getCurrentUser();

        return ResponseEntity.ok(capsuleCreateService.senderDelete(capsuleId, loginUser.memberId()));
    }

    // 캡슐 삭제 - 수신자
    @Operation(summary = "캡슐 삭제 API", description = "캡슐 수신자가 본인이 받은 캡슐을 저장 목록에서 삭제할 수 있도록 하는 API입니다.")
    @ApiErrorCodeExample({ ErrorCode.CAPSULE_NOT_FOUND })
    @DeleteMapping("/delete/reciver")
    public ResponseEntity<CapsuleDeleteResponseDTO> reciverDeleteCapsule(
            @RequestParam Long capsuleId
    ){
        // 현재 로그인 한 사용자 id
        InServerMemberResponse loginUser = currentUserContext.getCurrentUser();

        return ResponseEntity.ok(capsuleCreateService.receiverDelete(capsuleId, loginUser.phoneHash()));
    }
}