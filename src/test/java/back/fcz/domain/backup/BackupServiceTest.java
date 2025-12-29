package back.fcz.domain.backup;

import back.fcz.domain.backup.dto.response.GoogleDriveConnectionResponse;
import back.fcz.domain.backup.entity.Backup;
import back.fcz.domain.backup.repository.BackupRepository;
import back.fcz.domain.backup.service.BackupService;
import back.fcz.domain.backup.service.GoogleDriveService;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.domain.backup.service.GoogleTokenRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BackupServiceTest {
    @InjectMocks
    private BackupService backupService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CapsuleRepository capsuleRepository;
    @Mock
    private CapsuleRecipientRepository capsuleRecipientRepository;
    @Mock
    private PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    @Mock
    private BackupRepository backupRepository;
    @Mock
    private GoogleDriveService googleDriveService;
    @Mock
    private GoogleTokenRedisService googleTokenRedisService;
    @Mock
    private PhoneCrypto phoneCrypto;

    private Member member;
    private Backup backup;
    private Capsule capsule;

    private final Long MEMBER_ID = 1L;
    private final Long CAPSULE_ID = 1L;
    private final String PHONE_HASH = "hash123";
    private final String ENCRYPTED_REFRESH_TOKEN = "encrypted-refresh-token";

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .phoneHash(PHONE_HASH)
                .build();

        capsule = Capsule.builder()
                .capsuleId(CAPSULE_ID)
                .build();

        backup = Backup.builder()
                .memberId(MEMBER_ID)
                .build();
        backup.updateRefreshToken(ENCRYPTED_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("수신받은 캡슐 또는 조회한 공개 캡슐이 아닐 경우, 구글 드라이브 백업 예외 발생")
    void backup_fail_if_not_recipient() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(backupRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(backup));
        given(capsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndRecipientPhoneHash(CAPSULE_ID, PHONE_HASH)).willReturn(false);
        given(publicCapsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndMemberId(CAPSULE_ID, MEMBER_ID)).willReturn(false);

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            backupService.backupCapsule(MEMBER_ID, CAPSULE_ID);
        });

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ONLY_RECIPIENT_CAN_BACKUP);
    }

    @Test
    @DisplayName("구글 드라이브와 연동되지 않았으면, NEED_CONNECT 상태 반환")
    void backup_status_need_connect() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(backupRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());
        given(googleDriveService.generateAuthUrl()).willReturn("test-url");

        // when
        GoogleDriveConnectionResponse response = backupService.backupCapsule(MEMBER_ID, CAPSULE_ID);

        // then
        assertEquals("NEED_CONNECT", response.status());
        assertEquals("test-url", response.authUrl());
    }

    @Test
    @DisplayName("수신받은 캡슐일 경우, 업로드 수행 및 SUCCESS를 반환")
    void backup_received_capsule_success() throws Exception {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(backupRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(backup));
        given(capsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndRecipientPhoneHash(CAPSULE_ID, PHONE_HASH)).willReturn(true);
        given(publicCapsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndMemberId(CAPSULE_ID, MEMBER_ID)).willReturn(false);
        given(capsuleRepository.findById(CAPSULE_ID)).willReturn(Optional.of(capsule));

        // when
        GoogleDriveConnectionResponse response = backupService.backupCapsule(MEMBER_ID, CAPSULE_ID);

        // then
        assertEquals("SUCCESS", response.status());
        verify(googleDriveService, times(1)).uploadCapsule(any(), any());
    }

    @Test
    @DisplayName("조회한 공개 캡슐일 경우, 업로드 수행 및 SUCCESS를 반환")
    void backup_received_public_capsule_success() throws Exception {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(backupRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(backup));
        given(capsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndRecipientPhoneHash(CAPSULE_ID, PHONE_HASH)).willReturn(false);
        given(publicCapsuleRecipientRepository.existsByCapsuleId_CapsuleIdAndMemberId(CAPSULE_ID, MEMBER_ID)).willReturn(true);
        given(capsuleRepository.findById(CAPSULE_ID)).willReturn(Optional.of(capsule));

        // when
        GoogleDriveConnectionResponse response = backupService.backupCapsule(MEMBER_ID, CAPSULE_ID);

        // then
        assertEquals("SUCCESS", response.status());
        verify(googleDriveService, times(1)).uploadCapsule(any(), any());
    }

    @Test
    @DisplayName("액세스 토큰은 Redis에, 암호화된 리프레시 토큰은 DB에 저장")
    void persistGoogleToken_success() {
        // given
        String accessToken = "access";
        String refreshToken = "refresh";
        String encryptedRefreshToken = "encrypted-refresh";
        long expiresIn = 3600L;

        given(backupRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(backup));
        given(phoneCrypto.encrypt(refreshToken)).willReturn(encryptedRefreshToken);

        // when
        backupService.persistGoogleToken(MEMBER_ID, accessToken, refreshToken, expiresIn);

        // then
        verify(googleTokenRedisService, times(1)).saveAccessToken(MEMBER_ID, accessToken, expiresIn);

        assertThat(backup.getRefreshToken()).isEqualTo(encryptedRefreshToken);
        verify(backupRepository, times(1)).save(backup);
    }
}
