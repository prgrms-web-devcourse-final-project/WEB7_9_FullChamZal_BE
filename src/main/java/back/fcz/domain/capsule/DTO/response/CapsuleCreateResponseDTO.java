package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.DTO.GPSDTO;
import back.fcz.domain.capsule.DTO.UnlockDTO;
import back.fcz.domain.capsule.DTO.letterCustomDTO;
import back.fcz.domain.capsule.entity.Capsule;

public record CapsuleCreateResponseDTO(
        Long memberId,
        Long capsuleId,
        String nickName,
        String title,
        String content,
        String visibility,
        String unlockType,

        UnlockDTO unlock,
        letterCustomDTO letter,

        int maxViewCount,
        int currentViewCount
){
    public static CapsuleCreateResponseDTO from(Capsule capsule) {

        UnlockDTO unlockDTO = new UnlockDTO(
                capsule.getUnlockAt(),             // LocalDateTime unlockAt
                capsule.getLocationName(),         // String location
                new GPSDTO(                        // GPSDTO gps
                        capsule.getLocationLat(),
                        capsule.getLocationLng()
                ),
                capsule.getLocationRadiusM()       // int viewingRadius
        );

        letterCustomDTO letterDTO = new letterCustomDTO(
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor()
        );

        return new CapsuleCreateResponseDTO(
                capsule.getMemberId().getMemberId(),
                capsule.getCapsuleId(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getVisibility(),
                capsule.getUnlockType(),
                unlockDTO,
                letterDTO,
                capsule.getMaxViewCount(),
                capsule.getCurrentViewCount()
        );
    }
}