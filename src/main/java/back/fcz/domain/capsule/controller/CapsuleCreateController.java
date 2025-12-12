package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleUpdateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleCreateResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleUpdateResponseDTO;
import back.fcz.domain.capsule.DTO.response.SecretCapsuleCreateResponseDTO;
import back.fcz.domain.capsule.service.CapsuleCreateService;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/capsule")
public class CapsuleCreateController {

    private final CapsuleCreateService capsuleCreateService;

    // 캡슐 생성
    // 공개 캡슐
    @PostMapping("/create/public")
    public ResponseEntity<CapsuleCreateResponseDTO> createPublicCapsule(
            @RequestBody CapsuleCreateRequestDTO requestDTO
            ){
        return ResponseEntity.ok(capsuleCreateService.publicCapsuleCreate(requestDTO));
    }

    // 비공개 캡슐
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

    // 캡슐 수정
    @PutMapping("/update")
    public ResponseEntity<CapsuleUpdateResponseDTO> updateCapsule(
            @RequestParam Long capsuleId,
            @RequestBody CapsuleUpdateRequestDTO requestDTO
    ){
        return ResponseEntity.ok(capsuleCreateService.updateCapsule(capsuleId, requestDTO));
    }

    // 캡슐 삭제

}