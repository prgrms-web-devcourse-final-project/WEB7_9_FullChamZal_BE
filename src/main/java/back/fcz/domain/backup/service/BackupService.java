package back.fcz.domain.backup.service;

import back.fcz.domain.backup.dto.response.GoogleDriveConnectionResponse;
import back.fcz.domain.backup.entity.Backup;
import back.fcz.domain.backup.repository.BackupRepository;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {
    private final MemberRepository memberRepository;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private final BackupRepository backupRepository;
    private final GoogleDriveService googleDriveService;
    private final GoogleTokenRedisService googleTokenRedisService;
    private final PhoneCrypto phoneCrypto;

    /**
     * 구글 드라이브 백업 요청 처리
     * - 사용자가 구글 드라이브와 연동되어 있는지 확인
     *   - Backup 정보가 없거나, refresh token 이 없는 경우, 미연동 상태로 판단
     * - 미연동 상태인 경우, 구글 연동을 위한 URL 반환
     * - 이미 연동된 경우, 요청한 캡슐을 구글 드라이브에 CSV 파일로 백업
     */
    @Transactional
    public GoogleDriveConnectionResponse backupCapsule(long memberId, long capsuleId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Optional<Backup> token = backupRepository.findByMemberId(memberId);

        // 구글 드라이브와 연동되지 않았거나 리프레시 토큰이 없는 경우, 구글 드라이브 연동 실시
        if (token.isEmpty() || token.get().getRefreshToken() == null) {
            String authUrl = googleDriveService.generateAuthUrl();
            return new GoogleDriveConnectionResponse("NEED_CONNECT", "구글 드라이브 연동 필요", authUrl);
        }

        // 사용자가 수신한 개인 캡슐 또는 조회한 공개 캡슐인지 검증
        boolean isRecipient = capsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndRecipientPhoneHash(capsuleId, member.getPhoneHash());
        boolean isPublicRecipient = publicCapsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndMemberId(capsuleId, memberId);
        if (!isRecipient && !isPublicRecipient) { throw new BusinessException(ErrorCode.ONLY_RECIPIENT_CAN_BACKUP); }

        // 구글 드라이브에 CSV 파일 형태로 캡슐 업로드
        try {
            Capsule capsule = capsuleRepository.findById(capsuleId).orElseThrow(
                    ()-> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

            googleDriveService.uploadCapsule(token.get(), capsule);
            return new GoogleDriveConnectionResponse("SUCCESS", "구글 드라이브 백업 완료", null);
        } catch (Exception e) {
            log.error("Google Drive 업로드 실패 - {}", e.getMessage(), e);

            String authUrl = googleDriveService.generateAuthUrl();
            return new GoogleDriveConnectionResponse("NEED_CONNECT", "구글 드라이브 연동 필요", authUrl);
        }
    }

    /**
     * 구글 드라이브 연동 완료 후,
     * 콜백으로 전달받은 토큰 정보 저장 또는 업데이트
     */
    @Transactional
    public void saveGoogleToken(Long memberId, String code) {
        Map<String, Object> tokenData = googleDriveService.getTokensFromGoogle(code);

        String accessToken = (String) tokenData.get("access_token");
        String refreshToken = (String) tokenData.get("refresh_token");
        log.info("Google Access Token present: {}", accessToken != null);
        log.info("Google Refresh Token present: {}", refreshToken != null);

        // 구글 access 토큰의 만료 시간 (기본값 1시간)
        Object expiresInObj = tokenData.get("expires_in");
        long expiresIn = (expiresInObj instanceof Number)
                ? ((Number) expiresInObj).longValue() : 3600L;

        // access, refresh 토큰 저장
        persistGoogleToken(memberId, accessToken, refreshToken, expiresIn);
    }

    @Transactional
    public void persistGoogleToken(Long memberId, String accessToken, String refreshToken, long expiresIn) {
        // access 토큰을 Redis에 저장
        googleTokenRedisService.saveAccessToken(memberId, accessToken, expiresIn);

        Backup backup = backupRepository.findByMemberId(memberId)
                .orElseGet(() -> Backup.builder().memberId(memberId).build());

        // refresh 토큰이 새로 발급된 경우에만 DB 저장 또는 업데이트
        if (refreshToken != null && !refreshToken.isBlank()) {
            String encryptedRefreshToken = phoneCrypto.encrypt(refreshToken);  // refresh 토큰 암호화
            backup.updateRefreshToken(encryptedRefreshToken);

            backupRepository.save(backup);
            log.info("Google Refresh Token 저장 완료. MemberId: {}", memberId);
        }
    }
}
