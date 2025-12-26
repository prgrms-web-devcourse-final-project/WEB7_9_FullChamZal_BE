package back.fcz.domain.backup.controller;

import back.fcz.domain.backup.dto.response.GoogleDriveConnectionResponse;
import back.fcz.domain.backup.service.BackupService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/backup/google-drive")
@Tag(
        name = "구글 드라이브 백업 API",
        description = "구글 드라이브 백업 관련 API"
)
public class BackupController {
    private final BackupService backupService;

    @Value("${cors.allowed-origins}")
    private String frontendDomain;

    @Operation(summary = "구글 드라이브 백업",
            description = "사용자가 수신한 캡슐을 구글 드라이브에 CSV 파일로 백업합니다.\n\n" +
                    "### [응답 상태 - status]\n" +
                    "1. **SUCCESS**: 구글 드라이브와 이미 연동된 상태이며, 즉시 백업이 완료됩니다.\n" +
                    "2. **NEED_CONNECT**: 구글 드라이브 연동이 필요한 상태입니다. 반환된 `authUrl`로 사용자를 리다이렉트 시켜주세요!\n\n" +
                    "### [주의사항]\n" +
                    "* 구글 로그인 사용자는 최초 로그인 시 연동이 진행되지만, " +
                    "최초 동의가 이루어지지 않았거나 권한이 만료/유실된 경우 NEED_CONNECT가 발생할 수 있습니다.")
    @ApiErrorCodeExample({
            ErrorCode.ONLY_RECIPIENT_CAN_BACKUP,
            ErrorCode.GOOGLE_DRIVE_UPLOAD_FAIL,
            ErrorCode.GOOGLE_TOKEN_UPDATE_FAIL,
            ErrorCode.CAPSULE_NOT_FOUND
    })
    @PostMapping("/{capsuleId}")
    public ResponseEntity<ApiResponse<GoogleDriveConnectionResponse>> capsuleBackup(
            @PathVariable Long capsuleId,
            @AuthenticationPrincipal Long memberId) {

        GoogleDriveConnectionResponse response = backupService.backupCapsule(memberId, capsuleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "구글 드라이브 OAuth 콜백 API",
            description = "프론트엔드에서 직접 호출하지 않는 API입니다.\n\n" +
                    "사용자가 구글 드라이브 연동을 마치면, 구글에서 해당 api로 인가 코드를 보내줍니다. " +
                    "백엔드 서버는 인가 코드를 이용해 연동 토큰을 DB에 저장한 뒤, 프론트엔드 대시보드 페이지로 리다이렉트 합니다.")
    @GetMapping("/connect/callback")
    public void callback(
            @RequestParam String code,
            @AuthenticationPrincipal Long memberId,
            HttpServletResponse response
    ) throws IOException {
        backupService.saveGoogleToken(memberId, code);
        response.sendRedirect(frontendDomain + "/dashboard");
    }
}
