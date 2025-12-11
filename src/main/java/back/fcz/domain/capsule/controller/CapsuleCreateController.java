package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleCreateResponseDTO;
import back.fcz.domain.capsule.service.CapsuleCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/capsule")
public class CapsuleCreateController {

    private final CapsuleCreateService capsuleCreateService;

    // 캡슐 생성
    // 공개 캡슐
    @PostMapping("/create/public")
    public ResponseEntity<CapsuleCreateResponseDTO> createPublicCapsulte(
            @RequestBody CapsuleCreateRequestDTO requestDTO
            ){
        return ResponseEntity.ok(capsuleCreateService.publicCapsuleCreate(requestDTO));
    }

    // 비공개 캡슐

    // 캡슐 수정

    // 캡슐 삭제

}