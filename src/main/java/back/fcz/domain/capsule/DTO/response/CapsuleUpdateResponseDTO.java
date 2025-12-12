package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.DTO.UnlockResponseDTO;
import back.fcz.domain.capsule.DTO.LetterCustomResponseDTO;

public record CapsuleUpdateResponseDTO(
        Long memberId,
        Long capsuleId,
        String nickName,
        String URL,
        String updatedTitle,
        String updatedContent,
        String visibility,
        String unlockType,
        UnlockResponseDTO unlock,
        LetterCustomResponseDTO letter,
        int maxViewCount
){ }
