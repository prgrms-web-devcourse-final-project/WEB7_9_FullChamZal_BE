package back.fcz.domain.backup.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Backup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backup_id")
    private Long backupId;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    // Google Drive API 호출을 위한 인증 키
    @Column(length = 512)
    private String accessToken;

    // access 토큰 만료 시, 새 토큰을 발급받기 위한 마스터 키
    @Column(length = 512)
    private String refreshToken;

    // access 토큰 만료일
    private LocalDateTime expiryDate;

    @Builder
    private Backup(Long memberId, String accessToken, String refreshToken, long expiresIn) {
        this.memberId = memberId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiryDate = LocalDateTime.now().plusSeconds(expiresIn);
    }

    public void updateAccessToken(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiryDate = LocalDateTime.now().plusSeconds(expiresIn);
    }

    public void updateAccessToken(String accessToken, LocalDateTime expiryDate) {
        this.accessToken = accessToken;
        this.expiryDate = expiryDate;
    }

    public void updateRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
    }
}
