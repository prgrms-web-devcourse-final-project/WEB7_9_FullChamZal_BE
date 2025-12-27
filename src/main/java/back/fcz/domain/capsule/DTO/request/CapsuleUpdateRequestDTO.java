package back.fcz.domain.capsule.DTO.request;

import java.util.List;

public record CapsuleUpdateRequestDTO(
   String title,
   String content,
   List<Long> attachmentIds
){ }
