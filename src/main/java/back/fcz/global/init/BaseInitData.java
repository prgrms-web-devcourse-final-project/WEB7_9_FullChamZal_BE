package back.fcz.global.init;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"local","dev"})
@Configuration
@RequiredArgsConstructor
public class BaseInitData implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            return;  // 이미 데이터가 있으면 실행 안 함
        }

        createTestMembers();
    }

    private void createTestMembers() {
        // 일반 회원 1
        createMember(
                "testuser",
                "password123",
                "홍길동",
                "테스터",
                "010-1234-5678",
                "USER"
        );

        // 일반 회원 2 (캡슐 테스트용)
        createMember(
                "user2",
                "password123",
                "김철수",
                "캡슐러버",
                "010-2345-6789",
                "USER"
        );

        // 관리자
        createMember(
                "admin",
                "admin123",
                "관리자",
                "어드민",
                "010-9999-9999",
                "ADMIN"
        );
    }

    private void createMember(String userId, String password, String name,
                              String nickname, String phone, String role) {
        String encrypted = phoneCrypto.encrypt(phone);
        String hash = phoneCrypto.hash(phone);

        Member member = Member.builder()
                .userId(userId)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .nickname(nickname)
                .phoneNumber(encrypted)
                .phoneHash(hash)
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.valueOf(role))
                .build();

        memberRepository.save(member);
    }
}
