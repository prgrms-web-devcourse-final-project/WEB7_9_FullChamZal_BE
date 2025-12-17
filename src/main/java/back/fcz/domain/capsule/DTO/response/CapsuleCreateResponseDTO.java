package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.DTO.GPSResponseDTO;
import back.fcz.domain.capsule.DTO.UnlockResponseDTO;
import back.fcz.domain.capsule.DTO.LetterCustomResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;

public record CapsuleCreateResponseDTO(
        Long memberId,
        Long capsuleId,
        String nickname,
        String title,
        String content,
        String visibility,
        String unlockType,

        UnlockResponseDTO unlock,
        LetterCustomResponseDTO letter,

        int maxViewCount,
        int currentViewCount
){
    public static CapsuleCreateResponseDTO from(Capsule capsule) {

        UnlockResponseDTO unlockDTO = new UnlockResponseDTO(
                capsule.getUnlockAt(),             // LocalDateTime unlockAt
                capsule.getUnlockUntil(),
                capsule.getLocationName(),         // String location
                new GPSResponseDTO(
                        // GPSDTO gps
                        capsule.getAddress(),
                        capsule.getLocationLat(),
                        capsule.getLocationLng()
                ),
                capsule.getLocationRadiusM()       // int viewingRadius
        );

        LetterCustomResponseDTO letterDTO = new LetterCustomResponseDTO(
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