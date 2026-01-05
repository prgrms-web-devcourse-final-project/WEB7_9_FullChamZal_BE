package back.fcz.domain.capsule.scheduler;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ViewCountSyncSchedulerTest {

    @Autowired
    private ViewCountSyncScheduler syncScheduler;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PhoneCrypto phoneCrypto;

    private static final String VIEW_COUNT_KEY_PREFIX = "capsule:view:";

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        capsuleRepository.deleteAll();
        memberRepository.deleteAll();

        // Redis 초기화
        Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 테스트 회원 생성
        testMember = createAndSaveMember("test", "테스터", "01012345678");
    }

    @AfterEach
    void tearDown() {
        capsuleRepository.deleteAll();
        memberRepository.deleteAll();

        // Redis 정리
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

    private Capsule createAndSavePrivateCapsule(Member creator, int maxViewCount) {
        Capsule capsule = Capsule.builder()
                .memberId(creator)
                .uuid(java.util.UUID.randomUUID().toString())
                .nickname(creator.getNickname())
                .content("개인 캡슐 내용")
                .capsuleColor("BLUE")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().minusDays(1))
                .maxViewCount(maxViewCount)
                .currentViewCount(0)
                .build();

        return capsuleRepository.save(capsule);
    }

    private Capsule createAndSavePublicCapsule(Member creator, int maxViewCount) {
        Capsule capsule = Capsule.builder()
                .memberId(creator)
                .uuid(java.util.UUID.randomUUID().toString())
                .nickname(creator.getNickname())
                .content("공개 캡슐 내용")
                .capsuleColor("GREEN")
                .capsulePackingColor("YELLOW")
                .visibility("PUBLIC")
                .unlockType("LOCATION")
                .locationLat(37.5665)
                .locationLng(126.9780)
                .locationRadiusM(100)
                .maxViewCount(maxViewCount)
                .currentViewCount(0)
                .build();

        return capsuleRepository.save(capsule);
    }

    private void setRedisViewCount(Long capsuleId, int count) {
        String key = VIEW_COUNT_KEY_PREFIX + capsuleId;
        redisTemplate.opsForValue().set(key, String.valueOf(count));
    }

    private int getRedisViewCount(Long capsuleId) {
        String key = VIEW_COUNT_KEY_PREFIX + capsuleId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    private boolean redisKeyExists(Long capsuleId) {
        String key = VIEW_COUNT_KEY_PREFIX + capsuleId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ========== 통합 테스트 ==========

    @Nested
    @DisplayName("일반 캡슐 (선착순 없음) 동기화 테스트")
    class GeneralCapsuleSyncTests {

        @Test
        @Order(1)
        @DisplayName("Redis 조회수가 DB에 정상 동기화됨")
        void syncRedisToDb_success() {
            // Given
            Capsule capsule = createAndSavePrivateCapsule(testMember, 0); // 선착순 X
            setRedisViewCount(capsule.getCapsuleId(), 5);

            // Redis 확인
            int redisBefore = getRedisViewCount(capsule.getCapsuleId());
            assertThat(redisBefore).isEqualTo(5);

            // DB는 아직 0
            Capsule dbBefore = capsuleRepository.findById(capsule.getCapsuleId()).orElseThrow();
            assertThat(dbBefore.getCurrentViewCount()).isEqualTo(0);

            // When - 배치 동기화 실행
            syncScheduler.syncViewCountsToDB();

            // Then - DB 업데이트 확인
            Capsule dbAfter = capsuleRepository.findById(capsule.getCapsuleId()).orElseThrow();
            assertThat(dbAfter.getCurrentViewCount()).isEqualTo(5);

            // Redis 키 삭제 확인
            boolean keyExists = redisKeyExists(capsule.getCapsuleId());
            assertThat(keyExists).isFalse();
        }

        @Test
        @Order(2)
        @DisplayName("여러 캡슐의 Redis 조회수가 동시에 동기화됨")
        void syncMultipleCapsules() {
            // Given
            Capsule capsule1 = createAndSavePrivateCapsule(testMember, 0);
            Capsule capsule2 = createAndSavePrivateCapsule(testMember, 0);
            Capsule capsule3 = createAndSavePrivateCapsule(testMember, 0);

            setRedisViewCount(capsule1.getCapsuleId(), 3);
            setRedisViewCount(capsule2.getCapsuleId(), 7);
            setRedisViewCount(capsule3.getCapsuleId(), 2);

            // When
            syncScheduler.syncViewCountsToDB();

            // Then
            Capsule db1 = capsuleRepository.findById(capsule1.getCapsuleId()).orElseThrow();
            Capsule db2 = capsuleRepository.findById(capsule2.getCapsuleId()).orElseThrow();
            Capsule db3 = capsuleRepository.findById(capsule3.getCapsuleId()).orElseThrow();

            assertThat(db1.getCurrentViewCount()).isEqualTo(3);
            assertThat(db2.getCurrentViewCount()).isEqualTo(7);
            assertThat(db3.getCurrentViewCount()).isEqualTo(2);

            // 모든 Redis 키 삭제 확인
            assertThat(redisKeyExists(capsule1.getCapsuleId())).isFalse();
            assertThat(redisKeyExists(capsule2.getCapsuleId())).isFalse();
            assertThat(redisKeyExists(capsule3.getCapsuleId())).isFalse();
        }

        @Test
        @Order(3)
        @DisplayName("Redis 조회수가 0이면 동기화 건너뜀")
        void skipZeroViewCount() {
            // Given
            Capsule capsule = createAndSavePrivateCapsule(testMember, 0);
            setRedisViewCount(capsule.getCapsuleId(), 0);

            // When
            syncScheduler.syncViewCountsToDB();

            // Then - DB는 여전히 0
            Capsule dbAfter = capsuleRepository.findById(capsule.getCapsuleId()).orElseThrow();
            assertThat(dbAfter.getCurrentViewCount()).isEqualTo(0);

            // Redis 키는 삭제됨
            assertThat(redisKeyExists(capsule.getCapsuleId())).isFalse();
        }

        @Test
        @Order(4)
        @DisplayName("캡슐이 DB에 없으면 동기화 실패하고 로그만 남김")
        void syncNonExistentCapsule() {
            // Given
            Long nonExistentId = 99999L;
            setRedisViewCount(nonExistentId, 10);

            // When - 예외 발생하지 않고 정상 실행
            syncScheduler.syncViewCountsToDB();

            // Then - Redis 키는 삭제됨
            assertThat(redisKeyExists(nonExistentId)).isFalse();
        }

        @Test
        @Order(5)
        @DisplayName("누적 조회수 반영 - 기존 DB 값 + Redis 값")
        void accumulateViewCount() {
            // Given
            Capsule capsule = createAndSavePrivateCapsule(testMember, 0);

            // DB에 이미 10이 있음
            capsule.increasedViewCount(10);
            capsuleRepository.save(capsule);

            // Redis에 5 추가
            setRedisViewCount(capsule.getCapsuleId(), 5);

            // When
            syncScheduler.syncViewCountsToDB();

            // Then - 10 + 5 = 15
            Capsule dbAfter = capsuleRepository.findById(capsule.getCapsuleId()).orElseThrow();
            assertThat(dbAfter.getCurrentViewCount()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("선착순 캡슐 동기화 테스트")
    class FirstComeCapsuleSyncTests {

        @Test
        @Order(1)
        @DisplayName("선착순 캡슐(maxViewCount > 0)은 동기화에서 제외됨")
        void skipFirstComeCapsule() {
            // Given
            Capsule firstComeCapsule = createAndSavePublicCapsule(testMember, 100); // 선착순 O

            // DB에 이미 50이 있음 (선착순으로 증가된 값)
            firstComeCapsule.increasedViewCount(50);
            capsuleRepository.save(firstComeCapsule);

            // Redis에 잘못 들어간 값
            setRedisViewCount(firstComeCapsule.getCapsuleId(), 20);

            // When
            syncScheduler.syncViewCountsToDB();

            // Then - DB는 변경되지 않음 (50 유지)
            Capsule dbAfter = capsuleRepository.findById(firstComeCapsule.getCapsuleId()).orElseThrow();
            assertThat(dbAfter.getCurrentViewCount()).isEqualTo(50);

            // Redis 키는 삭제됨
            assertThat(redisKeyExists(firstComeCapsule.getCapsuleId())).isFalse();
        }

        @Test
        @Order(2)
        @DisplayName("선착순 캡슐과 일반 캡슐 혼재 시 선택적 동기화")
        void selectiveSync() {
            // Given
            Capsule firstComeCapsule = createAndSavePublicCapsule(testMember, 100); // 선착순 O
            Capsule generalCapsule = createAndSavePrivateCapsule(testMember, 0);    // 선착순 X

            setRedisViewCount(firstComeCapsule.getCapsuleId(), 30);
            setRedisViewCount(generalCapsule.getCapsuleId(), 15);

            // When
            syncScheduler.syncViewCountsToDB();

            // Then
            // 선착순 캡슐: DB 변경 없음
            Capsule dbFirstCome = capsuleRepository.findById(firstComeCapsule.getCapsuleId()).orElseThrow();
            assertThat(dbFirstCome.getCurrentViewCount()).isEqualTo(0);

            // 일반 캡슐: DB 업데이트됨
            Capsule dbGeneral = capsuleRepository.findById(generalCapsule.getCapsuleId()).orElseThrow();
            assertThat(dbGeneral.getCurrentViewCount()).isEqualTo(15);

            // 둘 다 Redis 키 삭제
            assertThat(redisKeyExists(firstComeCapsule.getCapsuleId())).isFalse();
            assertThat(redisKeyExists(generalCapsule.getCapsuleId())).isFalse();
        }
    }

    @Nested
    @DisplayName("예외 상황 테스트")
    class ExceptionTests {

        @Test
        @Order(1)
        @DisplayName("Redis에 키가 없으면 동기화 작업 안 함")
        void noRedisKeys_nothingHappens() {
            // Given - Redis에 아무것도 없음
            Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
            assertThat(keys).isNullOrEmpty();

            // When - 예외 발생하지 않음
            syncScheduler.syncViewCountsToDB();

            // Then - 정상 종료
        }

        @Test
        @Order(2)
        @DisplayName("잘못된 Redis 값(숫자 아님)이 있어도 다른 캡슐 동기화는 계속됨")
        void invalidRedisValue_continuesSyncing() {
            // Given
            Capsule validCapsule = createAndSavePrivateCapsule(testMember, 0);
            Long invalidCapsuleId = 88888L;

            setRedisViewCount(validCapsule.getCapsuleId(), 5);
            redisTemplate.opsForValue().set(VIEW_COUNT_KEY_PREFIX + invalidCapsuleId, "invalid");

            // When - 예외 발생하지 않고 계속 진행
            syncScheduler.syncViewCountsToDB();

            // Then - 유효한 캡슐은 정상 동기화
            Capsule dbValid = capsuleRepository.findById(validCapsule.getCapsuleId()).orElseThrow();
            assertThat(dbValid.getCurrentViewCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("도메인 메서드 검증")
    class DomainMethodTests {

        @Test
        @Order(1)
        @DisplayName("Capsule.increaseViewCount() 메서드 정상 동작")
        void increaseViewCount_works() {
            // Given
            Capsule capsule = createAndSavePrivateCapsule(testMember, 0);
            assertThat(capsule.getCurrentViewCount()).isEqualTo(0);

            // When
            capsule.increasedViewCount(10);
            capsuleRepository.save(capsule);

            // Then
            Capsule updated = capsuleRepository.findById(capsule.getCapsuleId()).orElseThrow();
            assertThat(updated.getCurrentViewCount()).isEqualTo(10);
        }

        @Test
        @Order(2)
        @DisplayName("Capsule.increaseViewCount() 누적 호출")
        void increaseViewCount_accumulates() {
            // Given
            Capsule capsule = createAndSavePrivateCapsule(testMember, 0);

            // When
            capsule.increasedViewCount(5);
            capsule.increasedViewCount(3);
            capsule.increasedViewCount(7);
            capsuleRepository.save(capsule);

            // Then
            Capsule updated = capsuleRepository.findById(capsule.getCapsuleId()).orElseThrow();
            assertThat(updated.getCurrentViewCount()).isEqualTo(15);
        }

        @Test
        @Order(3)
        @DisplayName("Capsule.increaseViewCount(0) 호출 시 변경 없음")
        void increaseViewCount_zero_noChange() {
            // Given
            Capsule capsule = createAndSavePrivateCapsule(testMember, 0);
            capsule.increasedViewCount(10);
            capsuleRepository.save(capsule);

            // When
            capsule.increasedViewCount(0);
            capsuleRepository.save(capsule);

            // Then
            Capsule updated = capsuleRepository.findById(capsule.getCapsuleId()).orElseThrow();
            assertThat(updated.getCurrentViewCount()).isEqualTo(10);
        }
    }
}