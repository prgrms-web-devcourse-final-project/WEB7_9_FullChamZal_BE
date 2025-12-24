package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleLikeResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleLike;
import back.fcz.domain.capsule.repository.CapsuleLikeRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapsuleLikeServiceTest {
    @InjectMocks
    private CapsuleLikeService capsuleLikeService;

    @Mock
    private CapsuleRepository capsuleRepository;

    @Mock
    private CapsuleLikeRepository capsuleLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    private Member member;
    private Capsule capsule;

    @BeforeEach
    void setUp() {
        // 1. 좋아요를 누를 사용자 (필드에 바로 할당)
        this.member = Member.builder().nickname("좋아요누르는사람").build();
        ReflectionTestUtils.setField(member, "memberId", 1L); // 생성한 객체에 ID 주입

        // 2. 캡슐을 소유한 주인 (지역 변수로 생성)
        Member capsuleOwner = Member.builder().nickname("캡슐주인").build();
        ReflectionTestUtils.setField(capsuleOwner, "memberId", 2L); // capsuleOwner에 주입! (기존엔 this.member로 되어있었음)

        // 3. 캡슐 생성
        this.capsule = Capsule.builder()
                .likeCount(10)
                .visibility("PUBLIC")
                .build();

        // 캡슐의 PK 설정
        ReflectionTestUtils.setField(capsule, "capsuleId", 100L);

        // 캡슐의 주인(capsuleOwner) 설정
        ReflectionTestUtils.setField(capsule, "memberId", capsuleOwner);
    }

    @Test
    @DisplayName("좋아요 증가 성공 테스트")
    void likeUp_Success() {
        // given
        Long capsuleId = 100L;
        Long memberId = 1L;
        when(currentUserContext.getCurrentMemberId()).thenReturn(memberId);
        when(capsuleLikeRepository.existsByCapsuleIdMemberId(capsuleId, memberId)).thenReturn(false);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(capsuleRepository.findById(capsuleId)).thenReturn(Optional.of(capsule));

        // when
        capsuleLikeService.likeUp(capsuleId);

        // then
        verify(capsuleLikeRepository, times(1)).save(any(CapsuleLike.class));
        verify(capsuleRepository, times(1)).incrementLikeCount(capsuleId);
    }

    @Test
    @DisplayName("좋아요 감소 성공 테스트")
    void likeDown_Success() {
        // given
        Long capsuleId = 100L;
        Long memberId = 1L;

        // 1. 좋아요 객체 생성 (findBy 결과로 반환할 객체)
        CapsuleLike mockLike = CapsuleLike.builder()
                .capsuleId(capsule)
                .memberId(member)
                .build();

        // 2. Mock 설정
        when(currentUserContext.getCurrentMemberId()).thenReturn(memberId);
        when(capsuleRepository.findById(capsuleId)).thenReturn(Optional.of(capsule));

        // existsBy가 아니라 findBy를 사용하도록 수정
        when(capsuleLikeRepository.findByCapsuleId_CapsuleIdAndMemberId_MemberId(capsuleId, memberId))
                .thenReturn(Optional.of(mockLike));

        // when
        CapsuleLikeResponse response = capsuleLikeService.likeDown(capsuleId);

        // then
        // delete(entity) 호출 확인
        verify(capsuleLikeRepository, times(1)).delete(any(CapsuleLike.class));
        verify(capsuleLikeRepository, times(1)).flush();
        verify(capsuleRepository, times(1)).decrementLikeCount(capsuleId);

        assertThat(response.message()).isEqualTo("좋아요 감소처리 성공");
    }
}
