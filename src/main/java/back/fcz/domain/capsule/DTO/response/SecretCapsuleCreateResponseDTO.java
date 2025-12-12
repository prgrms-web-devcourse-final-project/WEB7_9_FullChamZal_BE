package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.DTO.GPSResponseDTO;
import back.fcz.domain.capsule.DTO.UnlockResponseDTO;
import back.fcz.domain.capsule.DTO.LetterCustomResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;

public record SecretCapsuleCreateResponseDTO (
        Long memberId,
        Long capsuleId,
        String nickName,
        String url,
        String capPW,
        String title,
        String content,
        String visibility,
        String unlockType,

        UnlockResponseDTO unlock,
        LetterCustomResponseDTO letter,

        int maxViewCount,
        int currentViewCount
){
    public static SecretCapsuleCreateResponseDTO from(Capsule capsule, String url, String password) {

        UnlockResponseDTO unlockDTO = new UnlockResponseDTO(
                capsule.getUnlockAt(),             // LocalDateTime unlockAt
                capsule.getLocationName(),         // String location
                new GPSResponseDTO(                        // GPSDTO gps
                        capsule.getLocationLat(),
                        capsule.getLocationLng()
                ),
                capsule.getLocationRadiusM()       // int viewingRadius
        );

        LetterCustomResponseDTO letterDTO = new LetterCustomResponseDTO(
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor()
        );

        return new SecretCapsuleCreateResponseDTO(
                capsule.getMemberId().getMemberId(),
                capsule.getCapsuleId(),
                capsule.getNickname(),
                url,
                password,
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
