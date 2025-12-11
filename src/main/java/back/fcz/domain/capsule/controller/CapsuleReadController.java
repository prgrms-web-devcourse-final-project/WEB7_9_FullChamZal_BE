package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.dto.request.CapsuleReadRequestDto;
import back.fcz.domain.capsule.dto.response.CapsuleReadResponseDto;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("/api/v1/capsule")
public class CapsuleReadController {
    private final CapsuleReadService capsuleReadService;
    private final CapsuleRecipientRepository capsuleRecipientRepository;


    //GetMapping이 아닌이유는 phoneNumber를 url에 노출시키지 않으려고 @RequestBody를
    //사용 하였는데 GetMapping에서 @RequestBody를 쓰는 것이 Http표준에 위배되어서 PostMapping사용


    // 조건 검증 + 읽기
    @PostMapping("/{capsuleId}")
    public ResponseEntity<CapsuleReadResponseDto> readCapsule(
            @RequestBody CapsuleReadRequestDto capsuleReadRequestDto
    ) {
        Capsule resultCapsule = capsuleReadService.getCapsule(capsuleReadRequestDto.capsuleId());
        //isProtected확인(0이면 수신자가 비회원  /  1이면 수신자가 회원)
        if(resultCapsule.isProtected()){  //회원

            //jwt 페이로드에서 member_id 추출(유틸 함수가 있다고 가정)
            //이 member_id는 수신자의 id
            //Long memberId = JwtUtil.getMemberId();

            if(capsuleReadService.senderVerification(resultCapsule, capsuleReadRequestDto.phoneNumber(),
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
            //1. 비밀번호 검증
            capsuleReadRequestDto.password();
            //2. 해제 조건 검증
            //3. 저장 여부 선택
            //4. 저장 여부 선택에 따라 회원가입/로그인 권유
            //5. 캡슐 저장 처리
        }



        //사용자가 열람한 적이 있는 캡슐이라면 조건없이 읽기


        return ResponseEntity.ok(new CapsuleReadResponseDto(dto));




    }

    //사용자가 열람한 적이 있는 캡슐이라면 조건없이 읽기(이건 그냥 읽기)


    //회원의 대시보드
}
