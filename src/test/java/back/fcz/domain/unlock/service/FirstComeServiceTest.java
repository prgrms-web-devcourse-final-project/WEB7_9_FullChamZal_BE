package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class FirstComeServiceTest {

    @Autowired
    private FirstComeService firstComeService;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private Capsule testCapsule;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .userId("testuser")
                .passwordHash("password")
                .name("테스트")
                .nickname("테스터")
                .phoneNumber("01012345678")
                .phoneHash("hash")
                .build();
        memberRepository.save(testMember);

        // 선착순 3명 제한 캡슐 생성
        testCapsule = Capsule.builder()
                .memberId(testMember)
                .uuid("test-uuid")
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
                .uuid("test-uuid-2")
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
    @DisplayName("선착순 조회수 증가 성공")
    void tryIncrementViewCount_success() {
        // when
        firstComeService.tryIncrementViewCount(testCapsule.getCapsuleId());

        // then
        Capsule updated = capsuleRepository.findById(testCapsule.getCapsuleId()).orElseThrow();
        assertThat(updated.getCurrentViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("선착순 마감 시 예외 발생")
    void tryIncrementViewCount_closed() {
        // given - 이미 3명이 조회함
        for (int i = 0; i < 3; i++) {
            firstComeService.tryIncrementViewCount(testCapsule.getCapsuleId());
        }

        // when & then
        assertThatThrownBy(() -> firstComeService.tryIncrementViewCount(testCapsule.getCapsuleId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FIRST_COME_CLOSED);
    }

    @Test
    @DisplayName("남은 선착순 인원 조회")
    void getRemainingCount() {
        // given
        firstComeService.tryIncrementViewCount(testCapsule.getCapsuleId());
        Capsule updated = capsuleRepository.findById(testCapsule.getCapsuleId()).orElseThrow();

        // when
        int remaining = firstComeService.getRemainingCount(updated);

        // then
        assertThat(remaining).isEqualTo(2); // 3명 중 1명 사용, 2명 남음
    }
}