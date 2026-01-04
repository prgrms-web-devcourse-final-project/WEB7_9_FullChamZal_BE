package back.fcz.domain.capsule.DTO.request;

import java.time.LocalDateTime;

public record CapsuleConditionRequestDTO(
        Long capsuleId,
        //Integer isSendSelf,  // 타인에게 보내는 경우 0, 본인에게 보내는 경우 1
        LocalDateTime unlockAt,
        Double locationLat,
        Double locationLng,
        String password,
        String userAgent, // 서버에서 채우는 값: 브라우저 정보
        String ipAddress, // 서버에서 채우는 값: 클라이언트 IP 주소
        LocalDateTime serverTime // 서버에서 채우는 값: 현재 서버 시간
){
}

