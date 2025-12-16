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
import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
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
    private final ReportRepository reportRepository;



    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() == 0) {
            createTestMembers();
        }

        if (phoneVerificationRepository.count() == 0) {
            createTestPhoneVerifications();
        }

        if (reportRepository.count() == 0) {
            createDummyReports();
        }

        if (capsuleRepository.count() == 0) {
            createTestCapsules();
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
                MemberStatus.ACTIVE,
                MemberRole.USER
        );

        // 일반 회원 2 (캡슐 테스트용)
        createMember(
                "user2",
                "password123",
                "김철수",
                "캡슐러버",
                "010-2345-6789",
                MemberStatus.ACTIVE,
                MemberRole.USER
        );

        // 관리자
        createMember(
                "admin",
                "admin123",
                "관리자",
                "어드민",
                "010-9999-9999",
                MemberStatus.ACTIVE,
                MemberRole.ADMIN
        );

        // 정지 회원
        createMember(
                "stoppedUser",
                "test1234",
                "정지유저",
                "STOP_USER",
                "010-1111-2222",
                MemberStatus.STOP,
                MemberRole.USER
        );
    }
    private void createTestPhoneVerifications() {
        LocalDateTime now = LocalDateTime.now();

        // 성공적으로 인증된 번호
        createPhoneVerification(
                "010-1234-5678",
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
                "010-0012-3456",
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
                "010-0001-2345",
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
                "010-0000-1234",
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

    private void createTestCapsules() {
        List<Member> members = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == MemberRole.USER)
                .toList();

        if (members.size() < 2) return;

        Member member1 = members.get(0); // id 1
        Member member2 = members.get(1); // id 2

        Random random = new Random();

        for (int i = 1; i <= 20; i++) {

            boolean isPublic = i % 2 == 0; // 짝수 = PUBLIC, 홀수 = PRIVATE
            Member writer = (i % 2 == 0) ? member1 : member2;

            Capsule capsule = Capsule.builder()
                    .memberId(writer)
                    .uuid(UUID.randomUUID().toString())
                    .nickname(writer.getNickname())
                    .title("테스트 캡슐 " + i)
                    .content("테스트 캡슐 내용입니다. 번호: " + i)
                    .capPassword(isPublic ? null : "1234")
                    .capsuleColor("WHITE")
                    .capsulePackingColor("BLUE")
                    .visibility(isPublic ? "PUBLIC" : "PRIVATE")
                    .unlockType(isPublic ? "TIME" : "LOCATION")
                    .unlockAt(isPublic ? LocalDateTime.now().plusDays(i) : null)
                    .locationName(isPublic ? null : "테스트 장소 " + i)
                    .locationLat(isPublic ? null : 37.5 + random.nextDouble())
                    .locationLng(isPublic ? null : 127.0 + random.nextDouble())
                    .locationRadiusM(isPublic ? 0 : 100)
                    .maxViewCount(isPublic ? 0 : 1)
                    .currentViewCount(0)
                    .isDeleted(0)
                    .isProtected(isPublic ? 0 : 1)
                    .build();

            capsuleRepository.save(capsule);

            // 🔸 PRIVATE 캡슐이면 CapsuleRecipient 생성
            if (!isPublic) {
                Member recipient = writer == member1 ? member2 : member1;

                CapsuleRecipient capsuleRecipient = CapsuleRecipient.builder()
                        .capsuleId(capsule)
                        .recipientName(recipient.getName())
                        .recipientPhone(recipient.getPhoneNumber())
                        .recipientPhoneHash(recipient.getPhoneHash())
                        .isSenderSelf(0)
                        .unlockedAt(null)
                        .build();

                capsuleRecipientRepository.save(capsuleRecipient);
            }
        }
    }


}
