package back.fcz.domain.capsule.DTO.request;

import java.time.LocalDateTime;

public record CapsuleConditionRequestDTO(
        Long capsuleId,
        //Long memberId, // 열람을 시도하는 사람의 id
        String viewerType,  //MEMBER(회원), GUEST_AUTH(인증한 비회원), GUEST_UNAUTH(인증 안한 비회원)
        Integer isSendSelf,  // 타인에게 보내는 경우 0, 본인에게 보내는 경우 1
        //String phoneNumber,
        LocalDateTime unlockAt,
        Double locationLat,
        Double locationLng,

        String url,
        String password
){
}

