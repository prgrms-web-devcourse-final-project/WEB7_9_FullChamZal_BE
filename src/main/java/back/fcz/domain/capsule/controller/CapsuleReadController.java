package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleSendDashBoardRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleSendDashBoardResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.service.CapsuleDashBoardService;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    //컨트롤러에 있는 비지니스 로직을 서비스로 옮기기
    @GetMapping("/send/dashboard")
    public ResponseEntity<CapsuleSendDashBoardResponseDTO> readSendCapsule(
            @RequestBody CapsuleSendDashBoardRequestDTO capsuleSendDashBoardRequestDto
            ) {
        List<Capsule> capsules = capsuleDashBoardService.readSendCapsuleList(capsuleSendDashBoardRequestDto.memberId());
        List<CapsuleConditionResponseDTO> capsuleDtoList = new ArrayList<>();

        for(Capsule capsule : capsules){
            //응답 Dto생성
            CapsuleRecipient capsuleRecipient = capsuleRecipientRepository.findByCapsuleId_CapsuleId(capsule.getCapsuleId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

            CapsuleConditionResponseDTO capsuleConditionResponseDto = new CapsuleConditionResponseDTO(
                    capsule.getCapsuleId(),
                    capsule.getCapsuleColor(),
                    capsule.getCapsulePackingColor(),
                    capsuleRecipient.getRecipientName(),
                    capsule.getNickname(),
                    capsule.getTitle(),
                    capsule.getContent(),
                    capsule.getCreatedAt(),
                    capsule.getCurrentViewCount() > 0,
                    capsule.getUnlockType(),
                    capsule.getUpdatedAt(),
                    capsule.getLocationName(),
                    capsule.getLocationLat(),
                    capsule.getLocationLng()
            );
            capsuleDtoList.add(capsuleConditionResponseDto);
        }

        return  ResponseEntity.ok(new CapsuleSendDashBoardResponseDTO(capsuleDtoList));
    }

    //회원이 받은 캡슐의 대시보드 api


    //캡슐 저장버튼을 눌렀을때 호출할 api
    //수신자 테이블에 저장하는게 맞음

/*

    //사용자가 열람한 적이 있는 캡슐이라면 조건없이 읽기(시간, 공간 조건을 통과해서 조회한 적이 있는 경우에만 호출)
    @PostMapping("")
    public ResponseEntity<CapsuleReadResponseDTO> readCapsule(
            @RequestBody CapsuleReadRequestDTO capsuleReadRequestDto
    ){
        return ResponseEntity.ok(capsuleReadService.readCapsule(capsuleReadRequestDto.memberId(), capsuleReadRequestDto.capsuleId()));
    }
*/


}
