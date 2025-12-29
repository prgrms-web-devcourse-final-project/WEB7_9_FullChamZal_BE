package back.fcz.domain.backup.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // access 토큰 만료 시, 새 토큰을 발급받기 위한 마스터 키
    @Column(length = 512)
    private String refreshToken;

    @Builder
    private Backup(Long memberId) {
        this.memberId = memberId;
    }

    public void updateRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
    }
}
