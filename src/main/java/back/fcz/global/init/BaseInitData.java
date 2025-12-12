package back.fcz.global.init;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.report.entity.Report;
import back.fcz.domain.report.entity.ReportReasonType;
import back.fcz.domain.report.entity.ReportStatus;
import back.fcz.domain.report.repository.ReportRepository;
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
    private final ReportRepository reportRepository;


    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() == 0) {
            createTestMembers();
        }

        if (capsuleRepository.count() == 0) {
            createDummyCapsules();
        }

        if (reportRepository.count() == 0) {
            createDummyReports();
        }
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

    private void createDummyReports() {
        List<Capsule> capsules = capsuleRepository.findAll();
        if (capsules.isEmpty()) return;

        List<Member> members = memberRepository.findAll();

        // 관리자 찾기 (없으면 그냥 처리자 null로 들어가게)
        Member admin = members.stream()
                .filter(m -> m.getRole() == MemberRole.ADMIN)
                .findFirst()
                .orElse(null);

        // 일반 유저 목록
        List<Member> users = members.stream()
                .filter(m -> m.getRole() == MemberRole.USER)
                .toList();

        Random random = new Random();

        // 1) PENDING (회원 신고)
        {
            Capsule target = capsules.get(0);
            Member reporter = users.isEmpty() ? null : users.get(0);

            Report r = Report.builder()
                    .capsule(target)
                    .reporter(reporter)
                    .reporterPhone(null)
                    .reasonType(ReportReasonType.SPAM)
                    .reasonDetail("광고/홍보성 내용이 포함되어 있어요.")
                    .status(ReportStatus.PENDING)
                    .processedAt(null)
                    .processedBy(null)
                    .adminMemo(null)
                    .build();

            reportRepository.save(r);
        }

        // 2) REVIEWING (비회원 신고)
        {
            Capsule target = capsules.size() > 1 ? capsules.get(1) : capsules.get(0);

            String guestPhone = "010-7777-12" + String.format("%02d", random.nextInt(100));
            String encryptedGuestPhone = phoneCrypto.encrypt(guestPhone);

            Report r = Report.builder()
                    .capsule(target)
                    .reporter(null)
                    .reporterPhone(encryptedGuestPhone)
                    .reasonType(ReportReasonType.OBSCENITY)
                    .reasonDetail("음란/선정적 표현이 포함된 것 같습니다.")
                    .status(ReportStatus.REVIEWING)
                    .processedAt(null)
                    .processedBy(null)
                    .adminMemo("확인 중")
                    .build();

            reportRepository.save(r);
        }

        // 3) ACCEPTED (관리자 처리 완료)
        {
            Capsule target = capsules.size() > 2 ? capsules.get(2) : capsules.get(0);
            Member reporter = users.isEmpty() ? null : users.get(Math.min(1, users.size() - 1));

            Report r = Report.builder()
                    .capsule(target)
                    .reporter(reporter)
                    .reporterPhone(null)
                    .reasonType(ReportReasonType.HATE)
                    .reasonDetail("혐오/비하 표현이 포함되어 있습니다.")
                    .status(ReportStatus.ACCEPTED)
                    .processedAt(LocalDateTime.now().minusHours(5))
                    .processedBy(admin.getMemberId())
                    .adminMemo("내용 확인됨 → 승인 처리")
                    .build();

            reportRepository.save(r);
        }

        // 4) REJECTED (관리자 기각)
        {
            Capsule target = capsules.size() > 3 ? capsules.get(3) : capsules.get(0);

            String guestPhone = "010-8888-34" + String.format("%02d", random.nextInt(100));
            String encryptedGuestPhone = phoneCrypto.encrypt(guestPhone);

            Report r = Report.builder()
                    .capsule(target)
                    .reporter(null)
                    .reporterPhone(encryptedGuestPhone)
                    .reasonType(ReportReasonType.ETC)
                    .reasonDetail("그냥 기분이 나빠요.")
                    .status(ReportStatus.REJECTED)
                    .processedAt(LocalDateTime.now().minusDays(1))
                    .processedBy(admin.getMemberId())
                    .adminMemo("사유 불충분 → 기각")
                    .build();

            reportRepository.save(r);
        }
    }

}
