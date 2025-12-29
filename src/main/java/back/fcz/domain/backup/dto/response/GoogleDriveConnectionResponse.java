package back.fcz.domain.backup.dto.response;

public record GoogleDriveConnectionResponse(
        String status,        // "SUCCESS", "NEED_CONNECT"
        String description,   // status 설명
        String authUrl        // NEED_CONNECT일 경우에만 구글 인증 URL 포함
) { }
