package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("redis")
class FirstComeServiceTest {

    @Autowired
    private FirstComeService firstComeService;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;

    private Member testMember;
    private Capsule testCapsule;

    @BeforeEach
    void setUp() {
        // 고유한 전화번호 해시 생성 (UUID 사용)
        String uniqueHash = "hash-" + java.util.UUID.randomUUID().toString();
        String uniquePhone = "010" + System.currentTimeMillis(); // 타임스탬프로 고유성 보장

        // 테스트용 회원 생성
        testMember = Member.builder()
                .userId("testuser-" + System.currentTimeMillis()) // userId도 고유하게
                .passwordHash("password")
                .name("테스트")
                .nickname("테스터")
                .phoneNumber(uniquePhone)
                .phoneHash(uniqueHash)
                .build();
        memberRepository.save(testMember);

        // 선착순 3명 제한 캡슐 생성
        testCapsule = Capsule.builder()
                .memberId(testMember)
                .uuid("test-uuid-" + System.currentTimeMillis()) // UUID도 고유하게
                .nickname("테스터")
                .title("선착순 테스트")
                .content("선착순 3명")
                .visibility("PUBLIC")
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().minusDays(1))
                .capsuleColor("blue")
                .capsulePackingColor("red")
                .maxViewCount(3) // 선착순 3명
                .currentViewCount(0)
                .build();
        capsuleRepository.save(testCapsule);
    }

    @Test
    @DisplayName("선착순 제한이 있는 캡슐 확인")
    void hasFirstComeLimit_withLimit() {
        // when
        boolean result = firstComeService.hasFirstComeLimit(testCapsule);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("선착순 제한이 없는 캡슐 확인")
    void hasFirstComeLimit_withoutLimit() {
        // given
        testCapsule = Capsule.builder()
                .memberId(testMember)
                .uuid("test-uuid-unlimited-" + System.currentTimeMillis())
                .nickname("테스터")
                .title("무제한")
                .content("무제한")
                .visibility("PUBLIC")
                .unlockType("TIME")
                .unlockAt(LocalDateTime.now().minusDays(1))
                .capsuleColor("blue")
                .capsulePackingColor("red")
                .maxViewCount(0) // 무제한
                .currentViewCount(0)
                .build();
        capsuleRepository.save(testCapsule);

        // when
        boolean result = firstComeService.hasFirstComeLimit(testCapsule);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("선착순 조회수 증가 및 수신자 저장 성공")
    void tryIncrementViewCountAndSaveRecipient_success() {
        // when
        boolean result = firstComeService.tryIncrementViewCountAndSaveRecipient(
                testCapsule.getCapsuleId(),
                testMember.getMemberId(),
                LocalDateTime.now()
        );

        // then
        assertThat(result).isTrue();

        Capsule updated = capsuleRepository.findById(testCapsule.getCapsuleId()).orElseThrow();
        assertThat(updated.getCurrentViewCount()).isEqualTo(1);

        // PublicCapsuleRecipient도 저장되었는지 확인
        boolean exists = publicCapsuleRecipientRepository
                .existsByCapsuleId_CapsuleIdAndMemberId(testCapsule.getCapsuleId(), testMember.getMemberId());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("선착순 마감 시 예외 발생")
    void tryIncrementViewCountAndSaveRecipient_closed() {
        // given - 이미 3명이 조회함
        for (int i = 0; i < 3; i++) {
            Member member = Member.builder()
                    .userId("user" + i + "-" + System.currentTimeMillis())
                    .passwordHash("password")
                    .name("테스트" + i)
                    .nickname("테스터" + i)
                    .phoneNumber("010123456" + i + System.currentTimeMillis())
                    .phoneHash("hash" + i + "-" + java.util.UUID.randomUUID())
                    .build();
            memberRepository.save(member);

            firstComeService.tryIncrementViewCountAndSaveRecipient(
                    testCapsule.getCapsuleId(),
                    member.getMemberId(),
                    LocalDateTime.now()
            );
        }

        // when & then - 4번째 시도는 실패해야 함
        Member extraMember = Member.builder()
                .userId("extraUser-" + System.currentTimeMillis())
                .passwordHash("password")
                .name("추가")
                .nickname("추가")
                .phoneNumber("01099999999" + System.currentTimeMillis())
                .phoneHash("extraHash-" + java.util.UUID.randomUUID())
                .build();
        memberRepository.save(extraMember);

        assertThatThrownBy(() -> firstComeService.tryIncrementViewCountAndSaveRecipient(
                testCapsule.getCapsuleId(),
                extraMember.getMemberId(),
                LocalDateTime.now()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FIRST_COME_CLOSED);
    }

    @Test
    @DisplayName("남은 선착순 인원 조회")
    void getRemainingCount() {
        // given
        firstComeService.tryIncrementViewCountAndSaveRecipient(
                testCapsule.getCapsuleId(),
                testMember.getMemberId(),
                LocalDateTime.now()
        );
        Capsule updated = capsuleRepository.findById(testCapsule.getCapsuleId()).orElseThrow();

        // when
        int remaining = firstComeService.getRemainingCount(updated);

        // then
        assertThat(remaining).isEqualTo(2); // 3명 중 1명 사용, 2명 남음
    }

    @Test
    @DisplayName("이미 조회한 사용자는 재조회 시 false 반환")
    void tryIncrementViewCountAndSaveRecipient_alreadyViewed() {
        // given - 첫 조회
        firstComeService.tryIncrementViewCountAndSaveRecipient(
                testCapsule.getCapsuleId(),
                testMember.getMemberId(),
                LocalDateTime.now()
        );

        // when - 재조회
        boolean result = firstComeService.tryIncrementViewCountAndSaveRecipient(
                testCapsule.getCapsuleId(),
                testMember.getMemberId(),
                LocalDateTime.now()
        );

        // then
        assertThat(result).isFalse();

        // 조회수는 여전히 1이어야 함
        Capsule updated = capsuleRepository.findById(testCapsule.getCapsuleId()).orElseThrow();
        assertThat(updated.getCurrentViewCount()).isEqualTo(1);
    }
}