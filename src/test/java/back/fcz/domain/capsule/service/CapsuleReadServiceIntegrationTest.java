package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.entity.*;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CapsuleReadServiceIntegrationTest {

    @Autowired
    private CapsuleReadService capsuleReadService;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private CapsuleRecipientRepository capsuleRecipientRepository;

    @Autowired
    private CapsuleOpenLogRepository capsuleOpenLogRepository;

    @Autowired
    private PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PhoneCrypto phoneCrypto;

    private Member testSender;
    private Member testRecipient;
    private Member otherMember;

    private static final String VIEW_COUNT_KEY_PREFIX = "capsule:view:";

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        capsuleOpenLogRepository.deleteAll();
        publicCapsuleRecipientRepository.deleteAll();
        capsuleRecipientRepository.deleteAll();
        capsuleRepository.deleteAll();
        memberRepository.deleteAll();

        Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 테스트 회원 생성
        testSender = createAndSaveMember("sender", "발신자", "01011111111");
        testRecipient = createAndSaveMember("recipient", "수신자", "01022222222");
        otherMember = createAndSaveMember("other", "타인", "01033333333");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        capsuleOpenLogRepository.deleteAll();
        publicCapsuleRecipientRepository.deleteAll();
        capsuleRecipientRepository.deleteAll();
        capsuleRepository.deleteAll();
        memberRepository.deleteAll();

        Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ========== 헬퍼 메서드 ==========

    private Member createAndSaveMember(String userId, String nickname, String phone) {
        String encryptedPhone = phoneCrypto.encrypt(phone);
        String phoneHash = phoneCrypto.hash(phone);

        Member member = Member.create(
                userId,
                phoneCrypto.hash("password123"),
                nickname + "실명",
                nickname,
                encryptedPhone,
                phoneHash
        );

        return memberRepository.save(member);
    }

    private Capsule createAndSavePublicCapsule(Member creator, String unlockType) {
        Capsule capsule = Capsule.builder()
                .memberId(creator)
                .uuid(java.util.UUID.randomUUID().toString())
                .nickname(creator.getNickname())
                .content("공개 캡슐 내용")
                .capsuleColor("BLUE")
                .capsulePackingColor("RED")
                .visibility("PUBLIC")
                .unlockType(unlockType)
                .locationName("테스트 장소")
                .locationLat(37.5665)
                .locationLng(126.9780)
                .locationRadiusM(100)
                .unlockAt(LocalDateTime.now().minusDays(1)) // 이미 해제 가능
                .build();

        return capsuleRepository.save(capsule);
    }

    private Capsule createAndSaveProtectedPrivateCapsule(Member creator, Member recipient) {
        Capsule capsule = Capsule.builder()
                .memberId(creator)
                .uuid(java.util.UUID.randomUUID().toString())
                .nickname(creator.getNickname())
                .content("보호된 개인 캡슐 내용")
                .capsuleColor("GREEN")
                .capsulePackingColor("YELLOW")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().minusDays(1))
                .isProtected(1)
                .build();

        Capsule savedCapsule = capsuleRepository.save(capsule);

        // 수신자 정보 생성
        CapsuleRecipient capsuleRecipient = CapsuleRecipient.builder()
                .capsuleId(savedCapsule)
                .recipientName(recipient.getName())
                .recipientPhone(recipient.getPhoneNumber())
                .recipientPhoneHash(recipient.getPhoneHash())
                .isSenderSelf(0)
                .build();

        capsuleRecipientRepository.save(capsuleRecipient);

        return savedCapsule;
    }

    private Capsule createAndSaveUnprotectedPrivateCapsule(Member creator, String password) {
        String hashedPassword = phoneCrypto.hash(password);

        Capsule capsule = Capsule.builder()
                .memberId(creator)
                .uuid(java.util.UUID.randomUUID().toString())
                .nickname(creator.getNickname())
                .content("비보호 개인 캡슐 내용")
                .capsuleColor("PURPLE")
                .capsulePackingColor("PINK")
                .visibility("PRIVATE")
                .capPassword(hashedPassword)
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().minusDays(1))
                .isProtected(0)
                .build();

        return capsuleRepository.save(capsule);
    }

    private CapsuleConditionRequestDTO createRequestDto(Long capsuleId, String password) {
        return new CapsuleConditionRequestDTO(
                capsuleId,
                LocalDateTime.now(),
                37.5665,
                126.9780,
                password,
                "Mozilla/5.0",
                "192.168.1.1",
                LocalDateTime.now()
        );
    }

    private CapsuleConditionRequestDTO createRequestDtoWithIp(Long capsuleId, String password, String ipAddress) {
        return new CapsuleConditionRequestDTO(
                capsuleId,
                LocalDateTime.now(),
                37.5665,
                126.9780,
                password,
                "Mozilla/5.0",
                ipAddress,
                LocalDateTime.now()
        );
    }

    private void setupSecurityContext(Long memberId) {
        SecurityContextHolder.clearContext();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private int getRedisViewCount(Long capsuleId) {
        String key = VIEW_COUNT_KEY_PREFIX + capsuleId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    // ========== 공개 캡슐 통합 테스트 ==========

    @Nested
    @DisplayName("공개 캡슐 통합 테스트")
    class PublicCapsuleIntegrationTests {

        @Test
        @Order(1)
        @DisplayName("공개 캡슐 첫 조회 성공 - 실제 DB에 로그와 수신자 정보 저장")
        void publicCapsule_firstAccess_savesToDatabase() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            Capsule publicCapsule = createAndSavePublicCapsule(testSender, "LOCATION");
            CapsuleConditionRequestDTO requestDto = createRequestDto(publicCapsule.getCapsuleId(), null);

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);
            assertThat(result.capsuleId()).isEqualTo(publicCapsule.getCapsuleId());

            // 열람 로그 확인
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(CapsuleOpenStatus.SUCCESS);
            assertThat(logs.get(0).getViewerType()).isEqualTo("MEMBER");
            assertThat(logs.get(0).getMemberId()).isEqualTo(testRecipient.getMemberId());

            // 공개 캡슐 수신자 정보 확인
            List<PublicCapsuleRecipient> recipients = publicCapsuleRecipientRepository.findAll();
            assertThat(recipients).hasSize(1);
            assertThat(recipients.get(0).getMemberId()).isEqualTo(testRecipient.getMemberId());

            // Redis에서 조회수 확인
            int redisViewCount = getRedisViewCount(publicCapsule.getCapsuleId());
            assertThat(redisViewCount).isEqualTo(1);

            // DB엔 아직 조회수 0
            Capsule dbCapsule = capsuleRepository.findById(publicCapsule.getCapsuleId()).orElseThrow();
            assertThat(dbCapsule.getCurrentViewCount()).isEqualTo(0);
        }

        @Test
        @Order(2)
        @DisplayName("공개 캡슐 재조회 - 중복 수신자 정보 생성 안 됨, 조회수 증가 안 됨")
        void publicCapsule_reAccess_noDuplicateRecipient() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            Capsule publicCapsule = createAndSavePublicCapsule(testSender, "LOCATION");
            CapsuleConditionRequestDTO requestDto = createRequestDto(publicCapsule.getCapsuleId(), null);

            // 첫 조회
            capsuleReadService.conditionAndRead(requestDto);
            Capsule afterFirst = capsuleRepository.findById(publicCapsule.getCapsuleId()).orElseThrow();
            int firstRedisCount = getRedisViewCount(publicCapsule.getCapsuleId());

            // When - 재조회
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            // 수신자 정보는 1개만 생성되어야 함
            List<PublicCapsuleRecipient> recipients = publicCapsuleRecipientRepository.findAll();
            assertThat(recipients).hasSize(1);

            // 열람 로그는 2개 (첫 조회 + 재조회)
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(2);

            // 조회수는 증가하지 않음 (재조회이므로)
            int secondRedisCount = getRedisViewCount(publicCapsule.getCapsuleId());
            assertThat(secondRedisCount).isEqualTo(firstRedisCount);
        }

        @Test
        @Order(3)
        @DisplayName("공개 캡슐 비로그인 접근 시 예외 발생")
        void publicCapsule_withoutLogin_throwsException() {
            // Given
            SecurityContextHolder.clearContext();
            Capsule publicCapsule = createAndSavePublicCapsule(testSender, "LOCATION");
            CapsuleConditionRequestDTO requestDto = createRequestDto(publicCapsule.getCapsuleId(), null);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        @Order(4)
        @DisplayName("여러 회원이 동일한 공개 캡슐 조회 - 각각 수신자 정보 생성")
        void multipleMembers_accessSamePublicCapsule() {
            // Given
            Capsule publicCapsule = createAndSavePublicCapsule(testSender, "LOCATION");

            // When - 수신자1 조회
            setupSecurityContext(testRecipient.getMemberId());
            CapsuleConditionRequestDTO dto1 = createRequestDto(publicCapsule.getCapsuleId(), null);
            capsuleReadService.conditionAndRead(dto1);

            // When - 타인 조회
            setupSecurityContext(otherMember.getMemberId());
            CapsuleConditionRequestDTO dto2 = createRequestDto(publicCapsule.getCapsuleId(), null);
            capsuleReadService.conditionAndRead(dto2);

            // Then
            List<PublicCapsuleRecipient> recipients = publicCapsuleRecipientRepository.findAll();
            assertThat(recipients).hasSize(2);
            assertThat(recipients).extracting(PublicCapsuleRecipient::getMemberId)
                    .containsExactlyInAnyOrder(testRecipient.getMemberId(), otherMember.getMemberId());

            // 열람 로그도 2개
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(2);

            // 조회수는 2로 증가
            int redisViewCount = getRedisViewCount(publicCapsule.getCapsuleId());
            assertThat(redisViewCount).isEqualTo(2);
        }
    }

    // ========== 개인 캡슐 isProtected=1 통합 테스트 ==========

    @Nested
    @DisplayName("보호된 개인 캡슐 통합 테스트")
    class ProtectedPrivateCapsuleIntegrationTests {

        @Test
        @Order(1)
        @DisplayName("보호된 캡슐 - 수신자 본인 조회 성공")
        void protectedCapsule_validRecipient_success() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            Capsule protectedCapsule = createAndSaveProtectedPrivateCapsule(testSender, testRecipient);
            CapsuleConditionRequestDTO requestDto = createRequestDto(protectedCapsule.getCapsuleId(), null);

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);
            assertThat(result.capsuleId()).isEqualTo(protectedCapsule.getCapsuleId());

            // 열람 로그 확인
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(CapsuleOpenStatus.SUCCESS);

            // CapsuleRecipient의 unlockedAt 업데이트 확인
            CapsuleRecipient recipient = capsuleRecipientRepository
                    .findByCapsuleId_CapsuleId(protectedCapsule.getCapsuleId())
                    .orElseThrow();
            assertThat(recipient.getUnlockedAt()).isNotNull();

            // 조회수 증가 확인
            int redisViewCount = getRedisViewCount(protectedCapsule.getCapsuleId());
            assertThat(redisViewCount).isEqualTo(1);
        }

        @Test
        @Order(2)
        @DisplayName("보호된 캡슐 - 수신자가 아닌 회원 접근 시 예외 및 로그 기록")
        void protectedCapsule_invalidRecipient_throwsException() {
            // Given
            setupSecurityContext(otherMember.getMemberId());
            Capsule protectedCapsule = createAndSaveProtectedPrivateCapsule(testSender, testRecipient);
            CapsuleConditionRequestDTO requestDto = createRequestDto(protectedCapsule.getCapsuleId(), null);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CAPSULE_NOT_RECEIVER);

            // FAIL_PERMISSION 로그 확인
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(CapsuleOpenStatus.FAIL_PERMISSION);
            assertThat(logs.get(0).getMemberId()).isEqualTo(otherMember.getMemberId());
        }

        @Test
        @Order(3)
        @DisplayName("보호된 캡슐 재조회 - 조건 검증 생략, unlockedAt 재설정 안 됨")
        void protectedCapsule_reAccess_skipsValidation() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            Capsule protectedCapsule = createAndSaveProtectedPrivateCapsule(testSender, testRecipient);
            CapsuleConditionRequestDTO requestDto = createRequestDto(protectedCapsule.getCapsuleId(), null);

            // 첫 조회
            capsuleReadService.conditionAndRead(requestDto);

            CapsuleRecipient recipientAfterFirst = capsuleRecipientRepository
                    .findByCapsuleId_CapsuleId(protectedCapsule.getCapsuleId())
                    .orElseThrow();
            LocalDateTime firstUnlockedAt = recipientAfterFirst.getUnlockedAt();

            Capsule afterFirst = capsuleRepository.findById(protectedCapsule.getCapsuleId()).orElseThrow();
            int firstViewCount = afterFirst.getCurrentViewCount();

            // 잠시 대기
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When - 재조회
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            // 열람 로그 2개 확인
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(2);
            assertThat(logs).allMatch(log -> log.getStatus() == CapsuleOpenStatus.SUCCESS);

            // unlockedAt은 변경되지 않음
            CapsuleRecipient recipientAfterSecond = capsuleRecipientRepository
                    .findByCapsuleId_CapsuleId(protectedCapsule.getCapsuleId())
                    .orElseThrow();
            assertThat(recipientAfterSecond.getUnlockedAt()).isEqualTo(firstUnlockedAt);

            // 조회수는 증가하지 않음 (재조회이므로)
            Capsule afterSecond = capsuleRepository.findById(protectedCapsule.getCapsuleId()).orElseThrow();
            assertThat(afterSecond.getCurrentViewCount()).isEqualTo(firstViewCount);
        }

        @Test
        @Order(4)
        @DisplayName("보호된 캡슐 - 비로그인 상태로 접근 시 예외")
        void protectedCapsule_withoutLogin_throwsException() {
            // Given
            SecurityContextHolder.clearContext();
            Capsule protectedCapsule = createAndSaveProtectedPrivateCapsule(testSender, testRecipient);
            CapsuleConditionRequestDTO requestDto = createRequestDto(protectedCapsule.getCapsuleId(), null);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        @Order(5)
        @DisplayName("보호된 캡슐 재조회 - Redis 조회수 증가 안 됨")
        void protectedCapsule_reAccess_noRedisIncrement() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            Capsule protectedCapsule = createAndSaveProtectedPrivateCapsule(testSender, testRecipient);
            CapsuleConditionRequestDTO requestDto = createRequestDto(protectedCapsule.getCapsuleId(), null);

            // 첫 조회
            capsuleReadService.conditionAndRead(requestDto);
            int firstRedisCount = getRedisViewCount(protectedCapsule.getCapsuleId());

            // When - 재조회
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            // Redis 조회수는 증가하지 않음
            int secondRedisCount = getRedisViewCount(protectedCapsule.getCapsuleId());
            assertThat(secondRedisCount).isEqualTo(firstRedisCount);
        }
    }

    // ========== 개인 캡슐 isProtected=0 통합 테스트 ==========

    @Nested
    @DisplayName("비보호 개인 캡슐 통합 테스트")
    class UnprotectedPrivateCapsuleIntegrationTests {

        @Test
        @Order(1)
        @DisplayName("비보호 캡슐 - 회원 로그인 상태로 비밀번호 인증 성공")
        void unprotectedCapsule_memberWithValidPassword_success() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            String password = "secret123";
            Capsule unprotectedCapsule = createAndSaveUnprotectedPrivateCapsule(testSender, password);
            CapsuleConditionRequestDTO requestDto = createRequestDto(unprotectedCapsule.getCapsuleId(), password);

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);
            assertThat(result.capsuleId()).isEqualTo(unprotectedCapsule.getCapsuleId());

            // 열람 로그 확인 - MEMBER 타입
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(CapsuleOpenStatus.SUCCESS);
            assertThat(logs.get(0).getViewerType()).isEqualTo("MEMBER");
            assertThat(logs.get(0).getMemberId()).isNotNull();

            // 조회수 증가 확인
            int redisViewCount = getRedisViewCount(unprotectedCapsule.getCapsuleId());
            assertThat(redisViewCount).isEqualTo(1);
        }

        @Test
        @Order(2)
        @DisplayName("비보호 캡슐 - 비회원 상태로 비밀번호 인증 성공")
        void unprotectedCapsule_guestWithValidPassword_success() {
            // Given
            SecurityContextHolder.clearContext();
            String password = "secret123";
            Capsule unprotectedCapsule = createAndSaveUnprotectedPrivateCapsule(testSender, password);
            CapsuleConditionRequestDTO requestDto = createRequestDto(unprotectedCapsule.getCapsuleId(), password);

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(requestDto);

            // Then
            assertNotNull(result);

            // 열람 로그 확인 - GUEST 타입
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(CapsuleOpenStatus.SUCCESS);
            assertThat(logs.get(0).getViewerType()).isEqualTo("GUEST");
            assertThat(logs.get(0).getMemberId()).isNull();

            // 조회수 증가 확인
            int redisViewCount = getRedisViewCount(unprotectedCapsule.getCapsuleId());
            assertThat(redisViewCount).isEqualTo(1);
        }

        @Test
        @Order(3)
        @DisplayName("비보호 캡슐 - 비밀번호 불일치 시 예외 및 실패 로그")
        void unprotectedCapsule_invalidPassword_throwsException() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            String correctPassword = "secret123";
            Capsule unprotectedCapsule = createAndSaveUnprotectedPrivateCapsule(testSender, correctPassword);
            CapsuleConditionRequestDTO requestDto = createRequestDto(unprotectedCapsule.getCapsuleId(), "wrongPassword");

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CAPSULE_PASSWORD_NOT_MATCH);

            // FAIL_PASSWORD 로그 확인
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(CapsuleOpenStatus.FAIL_PASSWORD);
        }

        @Test
        @Order(4)
        @DisplayName("비보호 캡슐 - 비밀번호 미입력 시 예외")
        void unprotectedCapsule_noPassword_throwsException() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            Capsule unprotectedCapsule = createAndSaveUnprotectedPrivateCapsule(testSender, "secret123");
            CapsuleConditionRequestDTO requestDto = createRequestDto(unprotectedCapsule.getCapsuleId(), null);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.conditionAndRead(requestDto)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CAPSULE_PASSWORD_REQUIRED);
        }

        @Test
        @Order(5)
        @DisplayName("비보호 캡슐 재조회 - 회원은 memberId 기반, 조회수 증가 안 됨")
        void unprotectedCapsule_reAccess_memberBasedOnMemberId() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            String password = "secret123";
            Capsule unprotectedCapsule = createAndSaveUnprotectedPrivateCapsule(testSender, password);

            // 첫 조회
            CapsuleConditionRequestDTO firstDto = createRequestDtoWithIp(
                    unprotectedCapsule.getCapsuleId(), password, "192.168.1.1"
            );
            capsuleReadService.conditionAndRead(firstDto);

            Capsule afterFirst = capsuleRepository.findById(unprotectedCapsule.getCapsuleId()).orElseThrow();
            int firstRedisCount = getRedisViewCount(unprotectedCapsule.getCapsuleId());

            // When - 재조회 (다른 IP에서 접속)
            CapsuleConditionRequestDTO reAccessDto = createRequestDtoWithIp(
                    unprotectedCapsule.getCapsuleId(), password, "192.168.1.100"
            );
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(reAccessDto);

            // Then
            assertNotNull(result);

            // 열람 로그 2개 확인 (IP가 달라도 memberId 기반으로 재조회 판단)
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(2);

            // 조회수는 증가하지 않음 (재조회이므로)
            int secondRedisCount = getRedisViewCount(unprotectedCapsule.getCapsuleId());
            assertThat(secondRedisCount).isEqualTo(firstRedisCount);
        }

        @Test
        @Order(6)
        @DisplayName("비보호 캡슐 재조회 - 비회원은 IP 기반, 조회수 증가 안 됨")
        void unprotectedCapsule_reAccess_guestBasedOnIp() {
            // Given
            SecurityContextHolder.clearContext();
            String password = "secret123";
            Capsule unprotectedCapsule = createAndSaveUnprotectedPrivateCapsule(testSender, password);

            String firstIp = "192.168.1.1";
            CapsuleConditionRequestDTO firstDto = createRequestDtoWithIp(
                    unprotectedCapsule.getCapsuleId(), password, firstIp
            );

            // 첫 조회
            capsuleReadService.conditionAndRead(firstDto);

            Capsule afterFirst = capsuleRepository.findById(unprotectedCapsule.getCapsuleId()).orElseThrow();
            int firstRedisCount = getRedisViewCount(unprotectedCapsule.getCapsuleId());

            // When - 같은 IP에서 재조회
            CapsuleConditionResponseDTO result = capsuleReadService.conditionAndRead(firstDto);

            // Then
            assertNotNull(result);

            // 열람 로그 2개 확인
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(2);
            assertThat(logs).allMatch(log -> log.getIpAddress().equals(firstIp));

            // 조회수는 증가하지 않음 (재조회이므로)
            int secondRedisCount = getRedisViewCount(unprotectedCapsule.getCapsuleId());
            assertThat(secondRedisCount).isEqualTo(firstRedisCount);
        }

        @Test
        @Order(7)
        @DisplayName("비보호 캡슐 - 회원과 비회원이 각각 조회, 서로 다른 viewerType")
        void unprotectedCapsule_accessedByMemberAndGuest() {
            // Given
            String password = "secret123";
            Capsule unprotectedCapsule = createAndSaveUnprotectedPrivateCapsule(testSender, password);

            // When - 회원 조회
            setupSecurityContext(testRecipient.getMemberId());
            CapsuleConditionRequestDTO memberDto = createRequestDto(unprotectedCapsule.getCapsuleId(), password);
            capsuleReadService.conditionAndRead(memberDto);

            // When - 비회원 조회
            SecurityContextHolder.clearContext();
            CapsuleConditionRequestDTO guestDto = createRequestDtoWithIp(
                    unprotectedCapsule.getCapsuleId(), password, "192.168.1.100"
            );
            capsuleReadService.conditionAndRead(guestDto);

            // Then
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).hasSize(2);

            CapsuleOpenLog memberLog = logs.stream()
                    .filter(log -> "MEMBER".equals(log.getViewerType()))
                    .findFirst()
                    .orElseThrow();
            assertThat(memberLog.getMemberId()).isNotNull();

            CapsuleOpenLog guestLog = logs.stream()
                    .filter(log -> "GUEST".equals(log.getViewerType()))
                    .findFirst()
                    .orElseThrow();
            assertThat(guestLog.getMemberId()).isNull();

            // 조회수는 2로 증가 (각각 첫 조회)
            int redisViewCount = getRedisViewCount(unprotectedCapsule.getCapsuleId());
            assertThat(redisViewCount).isEqualTo(2);
        }
    }

    // ========== 작성자 자신의 캡슐 조회 통합 테스트 ==========

    @Nested
    @DisplayName("작성자 자신의 캡슐 조회 통합 테스트")
    class SelfCapsuleReadIntegrationTests {

        @Test
        @Order(1)
        @DisplayName("작성자가 자신의 공개 캡슐 조회 - 조건 검증 없이 성공")
        void selfRead_publicCapsule_success() {
            // Given
            setupSecurityContext(testSender.getMemberId());
            Capsule publicCapsule = createAndSavePublicCapsule(testSender, "TIME");

            // When
            CapsuleConditionResponseDTO result = capsuleReadService.capsuleRead(publicCapsule.getCapsuleId());

            // Then
            assertNotNull(result);
            assertThat(result.capsuleId()).isEqualTo(publicCapsule.getCapsuleId());

            // capsuleRead는 로그를 생성하지 않음
            List<CapsuleOpenLog> logs = capsuleOpenLogRepository.findAll();
            assertThat(logs).isEmpty();
        }

        @Test
        @Order(2)
        @DisplayName("작성자가 타인의 캡슐 조회 시도 - 예외 발생")
        void selfRead_othersCapsule_throwsException() {
            // Given
            setupSecurityContext(testRecipient.getMemberId());
            Capsule othersCapsule = createAndSavePublicCapsule(testSender, "TIME");

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> capsuleReadService.capsuleRead(othersCapsule.getCapsuleId())
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_SELF_CAPSULE);
        }
    }
}