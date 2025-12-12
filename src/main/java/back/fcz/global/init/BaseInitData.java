package back.fcz.global.init;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@Profile({"local","dev"})
@Configuration
@RequiredArgsConstructor
public class BaseInitData implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            return;  // 이미 데이터가 있으면 실행 안 함
        }

        createTestMembers();
        createDummyCapsules();
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

        // 정지 회원
        createMember(
                "stoppedUser",
                "test1234",
                "정지유저",
                "STOP_USER",
                "010-1111-2222",
                "USER"
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
    private void createPhoneVerification(String phoneNumber, String code, String purpose,
                                         String status, int attemptCount) {}

    private void createDummyCapsules() {
        List<Member> members = memberRepository.findAll();
        Random random = new Random();

        String[] visibilityOptions = {"PUBLIC", "PRIVATE"};
        String[] unlockTypes = {"TIME", "LOCATION", "TIME_AND_LOCATION"};

        for (int i = 1; i <= 20; i++) {

            Member owner = members.get(random.nextInt(members.size()));

            String visibility = visibilityOptions[random.nextInt(visibilityOptions.length)];
            String unlockType = unlockTypes[random.nextInt(unlockTypes.length)];

            boolean protectedCapsule = random.nextBoolean(); // true = isProtected=1

            Capsule capsule = Capsule.builder()
                    .memberId(owner)
                    .uuid(UUID.randomUUID().toString())
                    .nickname(owner.getNickname())
                    .title("더미 캡슐 제목 " + i)
                    .content("더미 캡슐 내용 " + i)
                    .capsuleColor(randomColor())
                    .capsulePackingColor(randomColor())
                    .visibility(visibility)
                    .unlockType(unlockType)
                    .unlockAt(unlockType.contains("TIME") ? LocalDateTime.now().plusDays(i) : null)
                    .locationName(unlockType.contains("LOCATION") ? "장소-" + i : null)
                    .locationLat(unlockType.contains("LOCATION") ? randomLat() : null)
                    .locationLng(unlockType.contains("LOCATION") ? randomLng() : null)
                    .locationRadiusM(unlockType.contains("LOCATION") ? 100 : 0)
                    .maxViewCount(0)
                    .currentViewCount(0)
                    .isDeleted(0)
                    .isProtected(protectedCapsule ? 1 : 0)
                    .build();

            // 보호 캡슐이면 비번 부여
            if (protectedCapsule) {
                String password = generateCapsulePassword();
                capsule.setCapPassword(password);
            }

            Capsule saved = capsuleRepository.save(capsule);


            // 보호 캡슐이면 수신자 생성
            if (protectedCapsule) {
                createRecipient(saved, i);
            }
        }
    }

    private String randomColor() {
        String[] colors = {"RED", "BLUE", "GREEN", "YELLOW", "PINK", "PURPLE"};
        return colors[new Random().nextInt(colors.length)];
    }

    private double randomLat() {
        return 37.5 + (Math.random() * 0.1); // 서울 근처 위도
    }

    private double randomLng() {
        return 127.0 + (Math.random() * 0.1); // 서울 근처 경도
    }

    private String generateCapsulePassword() {
        Random random = new Random();
        return String.valueOf(1000 + random.nextInt(9000)); // 4자리 숫자
    }

    private void createRecipient(Capsule capsule, int index) {
        String phone = "010-55" + (100 + index) + "-" + (1000 + index);

        String encrypted = phoneCrypto.encrypt(phone);
        String hash = phoneCrypto.hash(phone);

        CapsuleRecipient recipient = CapsuleRecipient.builder()
                .capsuleId(capsule)
                .recipientName("수신자 " + index)
                .recipientPhone(encrypted)
                .recipientPhoneHash(hash)
                .isSenderSelf(false)
                .build();

        capsuleRecipientRepository.save(recipient);
    }
}
