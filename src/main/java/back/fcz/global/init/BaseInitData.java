package back.fcz.global.init;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.report.entity.Report;
import back.fcz.domain.report.entity.ReportReasonType;
import back.fcz.domain.report.entity.ReportStatus;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackProgress;
import back.fcz.domain.storytrack.entity.StorytrackStep;
import back.fcz.domain.storytrack.repository.StorytrackProgressRepository;
import back.fcz.domain.storytrack.repository.StorytrackRepository;
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
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private final ReportRepository reportRepository;
    private final StorytrackRepository storytrackRepository;
    private final StorytrackProgressRepository storytrackProgressRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() == 0) {
            createTestMembers();
        }

        createFirstComeTestMembers();

        if (phoneVerificationRepository.count() == 0) {
            createTestPhoneVerifications();
        }

        if (reportRepository.count() == 0) {
            createDummyReports();
        }

        if (capsuleRepository.count() == 0) {
            createTestCapsules();
        }

        if (storytrackRepository.count() == 0) {
            createTestStorytracks();
        }

        createFirstComeTestCapsule();
    }

    private void createTestMembers() {
        // 일반 회원 1
        createMember(
                "testuser",
                "password123",
                "홍길동",
                "테스터",
                "01012345678",
                MemberStatus.ACTIVE,
                MemberRole.USER
        );

        // 일반 회원 2 (캡슐 테스트용)
        createMember(
                "user2",
                "password123",
                "김철수",
                "캡슐러버",
                "01023456789",
                MemberStatus.ACTIVE,
                MemberRole.USER
        );

        // 관리자
        createMember(
                "admin",
                "admin123",
                "관리자",
                "어드민",
                "01099999999",
                MemberStatus.ACTIVE,
                MemberRole.ADMIN
        );

        // 정지 회원
        createMember(
                "stoppedUser",
                "test1234",
                "정지유저",
                "STOP_USER",
                "01011112222",
                MemberStatus.STOP,
                MemberRole.USER
        );
    }
    private void createTestPhoneVerifications() {
        LocalDateTime now = LocalDateTime.now();

        // 성공적으로 인증된 번호
        createPhoneVerification(
                "01012345678",
                "123456",
                PhoneVerificationPurpose.SIGNUP,
                PhoneVerificationStatus.VERIFIED,
                1,
                now.minusMinutes(10),
                now.minusMinutes(7),
                now.minusMinutes(7)
        );
        // 인증하고 있는 중간 상태
        createPhoneVerification(
                "01000123456",
                "222333",
                PhoneVerificationPurpose.SIGNUP,
                PhoneVerificationStatus.PENDING,
                0,
                now.minusMinutes(2),
                null,
                now.plusMinutes(1)
        );


        // 만료된 인증 코드
        createPhoneVerification(
                "01000012345",
                "654321",
                PhoneVerificationPurpose.SIGNUP,
                PhoneVerificationStatus.EXPIRED,
                3,
                now.minusMinutes(10),
                null,
                now.minusMinutes(7)
        );

        // 시도 횟수 초과로 실패한 인증
        createPhoneVerification(
                "01000001234",
                "111222",
                PhoneVerificationPurpose.CHANGE_PHONE,
                PhoneVerificationStatus.EXPIRED,
                6,
                now.minusMinutes(10),
                null,
                now.minusMinutes(7)
        );
    }

    private void createMember(String userId, String password, String name,
                              String nickname, String phone, MemberStatus status, MemberRole role) {
        String encrypted = phoneCrypto.encrypt(phone);
        String hash = phoneCrypto.hash(phone);

        Member member = Member.builder()
                .userId(userId)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .nickname(nickname)
                .phoneNumber(encrypted)
                .phoneHash(hash)
                .status(status)
                .role(role)
                .build();

        memberRepository.save(member);
    }

    private void createPhoneVerification(String phoneNumber, String code, PhoneVerificationPurpose purpose,
                                         PhoneVerificationStatus status, int attemptCount, LocalDateTime createdAt,
                                         LocalDateTime verifiedAt, LocalDateTime expiredAt) {
        String phoneNumberHash = phoneCrypto.hash(phoneNumber);
        String codeHash = phoneCrypto.hash(code);
        LocalDateTime now = LocalDateTime.now();

        PhoneVerification phoneVerification = PhoneVerification.initForTest(
                phoneNumberHash,
                codeHash,
                purpose,
                status,
                attemptCount,
                createdAt,
                verifiedAt,
                expiredAt
        );

        phoneVerificationRepository.save(phoneVerification);
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

    private static final List<String> UNLOCK_TYPES =
            List.of("TIME", "LOCATION", "TIME_AND_LOCATION");

    private static final List<String> PUBLIC_UNLOCK_TYPES =
            List.of("LOCATION", "TIME_AND_LOCATION");

    private static final List<String> CAPSULE_COLORS =
            List.of("WHITE", "YELLOW", "PINK", "BLUE", "GREEN");

    private static final List<String> PACKING_COLORS =
            List.of("BLUE", "RED", "BROWN", "BLACK");

    private static final List<String> LOCATION_NAMES =
            List.of("서울역", "강남역", "한강공원", "우리 집", "학교 앞");

    private static final List<Double[]> LOCATIONS = List.of(
            new Double[]{37.5551, 126.9707}, // 서울역
            new Double[]{37.4979, 127.0276}, // 강남역
            new Double[]{37.5283, 126.9326}, // 한강
            new Double[]{37.5665, 126.9780}, // 시청
            new Double[]{37.4010, 127.1080}  // 분당
    );

    private <T> T randomFrom(List<T> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }

    private LocalDateTime randomUnlockAt(Random random) {
        int days = random.nextInt(21) - 7;
        int hours = random.nextInt(24);
        int minutes = random.nextInt(60);

        return LocalDateTime.now()
                .plusDays(days)
                .withHour(hours)
                .withMinute(minutes)
                .withSecond(0)
                .withNano(0);
    }

    private String randomPassword(Random random) {
        int pw = 1000 + random.nextInt(9000); // 1000 ~ 9999
        return String.valueOf(pw);
    }

    /* =========================
       공개 캡슐
       unlock: LOCATION / TIME_AND_LOCATION
       ========================= */
    private void createPublicCapsule(Member writer, int i, Random random) {

        String unlockType = randomFrom(PUBLIC_UNLOCK_TYPES, random);

        LocalDateTime unlockAt = null;
        String locationName = null;
        Double locationLat = null;
        Double locationLng = null;
        int locationRadius = 0;

        if (unlockType.contains("TIME")) {
            unlockAt = randomUnlockAt(random);
        }

        if (unlockType.contains("LOCATION")) {
            int idx = random.nextInt(LOCATIONS.size());
            Double[] loc = LOCATIONS.get(idx);

            locationName = LOCATION_NAMES.get(idx);
            locationLat = loc[0];
            locationLng = loc[1];
            locationRadius = randomFrom(List.of(50, 100, 300, 500), random);
        }

        int viewCount = (i % 2 == 1) ? 1 : 0;

        Capsule capsule = Capsule.builder()
                .memberId(writer)
                .uuid(UUID.randomUUID().toString())
                .nickname(writer.getNickname())
                .title("공개 캡슐 " + i)
                .content("공개 캡슐 테스트 데이터")
                .capsuleColor(randomFrom(CAPSULE_COLORS, random))
                .capsulePackingColor(randomFrom(PACKING_COLORS, random))
                .visibility("PUBLIC")
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .locationName(locationName)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .locationRadiusM(locationRadius)
                .currentViewCount(viewCount)
                .isProtected(0)
                .isDeleted(0)
                .build();

        Capsule saved = capsuleRepository.save(capsule);

        if (viewCount > 0) {
            PublicCapsuleRecipient recipient = PublicCapsuleRecipient.builder()
                    .capsuleId(saved)
                    .memberId(writer.getMemberId())
                    .unlockedAt(LocalDateTime.now().minusDays(random.nextInt(7)))
                    .build();

            publicCapsuleRecipientRepository.save(recipient);
        }
    }

    /* =========================
       비공개 캡슐 (비밀번호)
       unlock: TIME / LOCATION / TIME_AND_LOCATION
       ========================= */
    private void createPrivatePasswordCapsule(Member writer, int i, Random random) {

        String unlockType = randomFrom(UNLOCK_TYPES, random);

        LocalDateTime unlockAt = null;
        String locationName = null;
        Double locationLat = null;
        Double locationLng = null;
        int locationRadius = 0;

        if (unlockType.contains("TIME")) {
            unlockAt = randomUnlockAt(random);
        }

        if (unlockType.contains("LOCATION")) {
            int idx = random.nextInt(LOCATIONS.size());
            Double[] loc = LOCATIONS.get(idx);

            locationName = LOCATION_NAMES.get(idx);
            locationLat = loc[0];
            locationLng = loc[1];
            locationRadius = randomFrom(List.of(50, 100, 300, 500), random);
        }

        String rawPassword = randomPassword(random);
        int viewCount = (i % 2 == 0) ? 1 : 0;

        Capsule capsule = Capsule.builder()
                .memberId(writer)
                .uuid(UUID.randomUUID().toString())
                .nickname(writer.getNickname())
                .receiverNickname("비공개 수신자 " + i)
                .title("비공개(비밀번호) 캡슐 " + i)
                .content("비밀번호로 여는 캡슐")
                .capsuleColor(randomFrom(CAPSULE_COLORS, random))
                .capsulePackingColor(randomFrom(PACKING_COLORS, random))
                .visibility("PRIVATE")
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .locationName(locationName)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .locationRadiusM(locationRadius)
                .capPassword(phoneCrypto.hash("5678"))
                .currentViewCount(viewCount)
                .isProtected(0)
                .isDeleted(0)
                .build();

        capsuleRepository.save(capsule);
    }

    /* =========================
       비공개 캡슐 (회원 전화번호)
       ========================= */
    private void createPrivatePhoneMemberCapsule(
            Member writer, Member recipient, int i, Random random) {

        String unlockType = randomFrom(UNLOCK_TYPES, random);

        LocalDateTime unlockAt = null;
        String locationName = null;
        Double locationLat = null;
        Double locationLng = null;
        int locationRadius = 0;

        if (unlockType.contains("TIME")) {
            unlockAt = randomUnlockAt(random);
        }

        if (unlockType.contains("LOCATION")) {
            int idx = random.nextInt(LOCATIONS.size());
            Double[] loc = LOCATIONS.get(idx);

            locationName = LOCATION_NAMES.get(idx);
            locationLat = loc[0];
            locationLng = loc[1];
            locationRadius = randomFrom(List.of(50, 100, 300, 500), random);
        }

        int viewCount = (i % 2 == 1) ? 1 : 0;

        Capsule capsule = Capsule.builder()
                .memberId(writer)
                .uuid(UUID.randomUUID().toString())
                .nickname(writer.getNickname())
                .receiverNickname(recipient.getNickname())
                .title("비공개(전화번호) 캡슐 " + i)
                .content("회원 전화번호 수신 캡슐")
                .capsuleColor(randomFrom(CAPSULE_COLORS, random))
                .capsulePackingColor(randomFrom(PACKING_COLORS, random))
                .visibility("PRIVATE")
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .locationName(locationName)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .locationRadiusM(locationRadius)
                .currentViewCount(viewCount)
                .isProtected(1)
                .isDeleted(0)
                .build();

        Capsule saved = capsuleRepository.save(capsule);

        LocalDateTime unlockedAt = (viewCount > 0)
                ? LocalDateTime.now().minusDays(random.nextInt(7))
                : null;

        CapsuleRecipient recipientRecord = CapsuleRecipient.builder()
                .capsuleId(saved)
                .recipientName(recipient.getName())
                .recipientPhone(recipient.getPhoneNumber())
                .recipientPhoneHash(recipient.getPhoneHash())
                .unlockedAt(unlockedAt)
                .isSenderSelf(0)
                .build();

        capsuleRecipientRepository.save(recipientRecord);
    }

    /* =========================
       비공개 캡슐 (비회원 전화번호)
       ========================= */
    private void createPrivatePhoneGuestCapsule(Member writer, int i, Random random) {

        String unlockType = randomFrom(UNLOCK_TYPES, random);

        LocalDateTime unlockAt = null;
        String locationName = null;
        Double locationLat = null;
        Double locationLng = null;
        int locationRadius = 0;

        if (unlockType.contains("TIME")) {
            unlockAt = randomUnlockAt(random);
        }

        if (unlockType.contains("LOCATION")) {
            int idx = random.nextInt(LOCATIONS.size());
            Double[] loc = LOCATIONS.get(idx);

            locationName = LOCATION_NAMES.get(idx);
            locationLat = loc[0];
            locationLng = loc[1];
            locationRadius = randomFrom(List.of(50, 100, 300, 500), random);
        }

        int viewCount = (i % 2 == 0) ? 1 : 0;

        Capsule capsule = Capsule.builder()
                .memberId(writer)
                .uuid(UUID.randomUUID().toString())
                .nickname(writer.getNickname())
                .receiverNickname("전화번호 비회원 수신자 " + i)
                .title("비공개(비회원) 캡슐 " + i)
                .content("비회원 전화번호 캡슐")
                .capsuleColor(randomFrom(CAPSULE_COLORS, random))
                .capsulePackingColor(randomFrom(PACKING_COLORS, random))
                .visibility("PRIVATE")
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .locationName(locationName)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .locationRadiusM(locationRadius)
                .capPassword(phoneCrypto.hash("5678"))
                .currentViewCount(viewCount)
                .isProtected(0)
                .isDeleted(0)
                .build();

        capsuleRepository.save(capsule);
    }

    /* =========================
       나에게 보내는 캡슐
       ========================= */
    private void createCapsuleToMe(Member member, int i, Random random) {

        String unlockType = randomFrom(UNLOCK_TYPES, random);

        LocalDateTime unlockAt = null;

        if (unlockType.contains("TIME")) {
            unlockAt = randomUnlockAt(random);
        }

        int viewCount = (i % 2 == 0) ? 1 : 0;

        Capsule capsule = Capsule.builder()
                .memberId(member)
                .uuid(UUID.randomUUID().toString())
                .nickname(member.getNickname())
                .receiverNickname(member.getNickname())
                .title("나에게 쓰는 캡슐 " + i)
                .content("셀프 캡슐 테스트")
                .capsuleColor(randomFrom(CAPSULE_COLORS, random))
                .capsulePackingColor(randomFrom(PACKING_COLORS, random))
                .visibility("SELF")
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .currentViewCount(viewCount)
                .isProtected(1)
                .isDeleted(0)
                .build();

        Capsule saved = capsuleRepository.save(capsule);

        LocalDateTime unlockedAt = (viewCount > 0)
                ? LocalDateTime.now().minusDays(random.nextInt(7))
                : null;

        CapsuleRecipient recipientRecord = CapsuleRecipient.builder()
                .capsuleId(saved)
                .recipientName(member.getName())
                .recipientPhone(member.getPhoneNumber())
                .recipientPhoneHash(member.getPhoneHash())
                .unlockedAt(unlockedAt)
                .isSenderSelf(1)
                .build();

        capsuleRecipientRepository.save(recipientRecord);
    }

    /* =========================
       실행 엔트리
       ========================= */
    private void createTestCapsules() {
        List<Member> members = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == MemberRole.USER)
                .toList();

        if (members.size() < 2) return;

        Member member1 = members.get(0);
        Member member2 = members.get(1);

        Random random = new Random();

        for (int i = 1; i <= 20; i++) {
            int type = i % 5;

            switch (type) {
                case 0 -> createPublicCapsule(member1, i, random);
                case 1 -> createPrivatePasswordCapsule(member2, i, random);
                case 2 -> createPrivatePhoneMemberCapsule(member1, member2, i, random);
                case 3 -> createPrivatePhoneGuestCapsule(member2, i, random);
                case 4 -> createCapsuleToMe(member1, i, random);
            }
        }
    }

    /**
     * K6 선착순 테스트 전용 회원 100명 생성
     * - testuser1 ~ testuser100
     * - password: password123
     * - phone: 010-9999-0001 ~ 010-9999-0100
     */
    private void createFirstComeTestMembers() {

        // 이미 생성되어 있으면 다시 만들지 않음
        boolean exists = memberRepository.existsByUserId("testuser1");
        if (exists) {
            return;
        }

        for (int i = 1; i <= 100; i++) {
            String userId = "testuser" + i;
            String phone = String.format("010-9999-%04d", i);

            createMember(
                    userId,
                    "password123",
                    "선착순테스터" + i,
                    "fc_tester_" + i,
                    phone,
                    MemberStatus.ACTIVE,
                    MemberRole.USER
            );
        }
    }

    /**
     * K6 선착순 테스트 전용 공개 캡슐 생성
     */
    private void createFirstComeTestCapsule() {

        // 이미 존재하면 중복 생성 방지
        boolean exists = capsuleRepository.findAll().stream()
                .anyMatch(c -> "K6 선착순 테스트 캡슐".equals(c.getTitle()));

        if (exists) {
            return;
        }

        // 작성자: USER 중 아무나
        Member writer = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == MemberRole.USER)
                .findFirst()
                .orElse(null);

        if (writer == null) {
            return;
        }

        Capsule capsule = Capsule.builder()
                .memberId(writer)
                .uuid(UUID.randomUUID().toString())
                .nickname(writer.getNickname())
                .title("선착순 테스트 K6")
                .content("K6 동시성(선착순) 테스트 전용 캡슐입니다.")
                .capsuleColor("WHITE")
                .capsulePackingColor("BLUE")
                .visibility("PUBLIC")
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().minusDays(1))
                .maxViewCount(10)
                .currentViewCount(0)
                .isProtected(0)
                .isDeleted(0)
                .build();

        capsuleRepository.save(capsule);
    }

    private void createTestStorytracks() {

        List<Member> members = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == MemberRole.USER)
                .toList();

        if (members.size() < 2) return;

        Member creator = members.get(0);
        Member participant = members.get(1);

        // PUBLIC 캡슐 최소 3개만 있으면 생성
        List<Capsule> publicCapsules = capsuleRepository.findAll().stream()
                .filter(c -> "PUBLIC".equals(c.getVisibility()))
                .toList();

        if (publicCapsules.size() < 3) return;

    /* ======================
       SEQUENTIAL 스토리트랙 (앞 3개)
       ====================== */
        Storytrack sequential = Storytrack.builder()
                .member(creator)
                .title("테스트 스토리트랙")
                .description("SEQUENTIAL 테스트")
                .trackType("SEQUENTIAL")
                .isPublic(1)
                .price(0)
                .isDeleted(0)
                .build();

        storytrackRepository.save(sequential);

        int order = 1;
        for (Capsule capsule : publicCapsules.subList(0, 3)) {
            StorytrackStep step = StorytrackStep.builder()
                    .capsule(capsule)
                    .stepOrder(order++)
                    .build();
            sequential.addStep(step);
        }

        sequential.setTotalSteps(sequential.getSteps().size());
        storytrackRepository.save(sequential);

        storytrackProgressRepository.save(
                StorytrackProgress.builder()
                        .member(participant)
                        .storytrack(sequential)
                        .completedSteps(1)
                        .lastCompletedStep(1)
                        .build()
        );

    /* ======================
       FREE 스토리트랙 (남은 캡슐 최대 3개)
       ====================== */
        if (publicCapsules.size() < 4) return; // FREE는 최소 1개 이상일 때만

        Storytrack free = Storytrack.builder()
                .member(creator)
                .title("자유형 스토리트랙")
                .description("FREE 타입 테스트")
                .trackType("FREE")
                .isPublic(1)
                .price(0)
                .isDeleted(0)
                .build();

        storytrackRepository.save(free);

        order = 1;
        int fromIndex = 3;
        int toIndex = Math.min(publicCapsules.size(), 6); // 최대 3개

        for (Capsule capsule : publicCapsules.subList(fromIndex, toIndex)) {
            StorytrackStep step = StorytrackStep.builder()
                    .capsule(capsule)
                    .stepOrder(order++)
                    .build();
            free.addStep(step);
        }

        free.setTotalSteps(free.getSteps().size());
        storytrackRepository.save(free);

        storytrackProgressRepository.save(
                StorytrackProgress.builder()
                        .member(participant)
                        .storytrack(free)
                        .completedSteps(0)
                        .lastCompletedStep(0)
                        .build()
        );
    }



}
