package back.fcz.global.init;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.UUID;

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
        // SYSTEM 계정이 없으면 생성
        if (!memberRepository.existsById(0L)) {
            createSystemAdmin();
            log.info("✅ 시스템 관리자 계정 생성 완료 (자동 제재용)");
        } else {
            log.info("ℹ️ 시스템 관리자 계정이 이미 존재합니다");
        }
    }

    private void createSystemAdmin() {
        String systemPhone = "00000000000";
        String encrypted = phoneCrypto.encrypt(systemPhone);
        String hash = phoneCrypto.hash(systemPhone);

        Member systemAdmin = Member.builder()
                .userId("SYSTEM")
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .name("시스템")
                .nickname("SYSTEM")
                .phoneNumber(encrypted)
                .phoneHash(hash)
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.ADMIN)
                .build();

        // Reflection을 사용해 memberId를 0으로 강제 설정
        try {
            Field memberIdField = Member.class.getDeclaredField("memberId");
            memberIdField.setAccessible(true);
            memberIdField.set(systemAdmin, 0L);
        } catch (Exception e) {
            log.error("SYSTEM 계정 ID 설정 실패", e);
            throw new RuntimeException("시스템 관리자 계정 초기화 실패", e);
        }

        memberRepository.save(systemAdmin);
    }
}