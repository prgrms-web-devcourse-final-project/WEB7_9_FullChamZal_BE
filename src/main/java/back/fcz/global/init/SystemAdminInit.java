package back.fcz.global.init;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sanction.constant.SanctionConstants;
import back.fcz.global.crypto.PhoneCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 시스템 관리자 계정 자동 생성
@Slf4j
@Component
@Configuration
@RequiredArgsConstructor
public class SystemAdminInit implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 존재하면 생성하지 않음
        if (memberRepository.existsByUserId(SanctionConstants.SYSTEM_ADMIN_USER_ID)) {
            log.info("시스템 관리자 계정이 이미 존재합니다. (userId={})",
                    SanctionConstants.SYSTEM_ADMIN_USER_ID);
            return;
        }

        createSystemAdmin();
        log.info("시스템 관리자 계정 생성 완료 (userId={})",
                SanctionConstants.SYSTEM_ADMIN_USER_ID);
    }

    private void createSystemAdmin() {
        String systemPhone = "00000000000";
        String encrypted = phoneCrypto.encrypt(systemPhone);
        String hash = phoneCrypto.hash(systemPhone);

        Member systemAdmin = Member.builder()
                .userId(SanctionConstants.SYSTEM_ADMIN_USER_ID)
                .passwordHash(passwordEncoder.encode("SYSTEM_AUTO_GENERATED_" + System.currentTimeMillis()))
                .name("시스템")
                .nickname("시스템")
                .phoneNumber(encrypted)
                .phoneHash(hash)
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.ADMIN)
                .oauthProvider(null)
                .oauthId(null)
                .build();

        memberRepository.save(systemAdmin);
    }
}