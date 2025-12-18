package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleUpdateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleCreateResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleDeleteResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleUpdateResponseDTO;
import back.fcz.domain.capsule.DTO.response.SecretCapsuleCreateResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapsuleCreateServiceTest {

    @Mock
    CapsuleRepository capsuleRepository;
    @Mock
    CapsuleRecipientRepository recipientRepository;
    @Mock
    PublicCapsuleRecipientRepository publicRecipientRepository;
    @Mock
    MemberRepository memberRepository;
    @Mock
    PhoneCrypto phoneCrypto;
    @Mock
    CapsuleOpenLogRepository capsuleOpenLogRepository;

    @InjectMocks
    CapsuleCreateService capsuleCreateService;

    // 공통 Member 객체
    private Member member;

    @BeforeEach
    void setup() {
        member = Member.testMember(1L, "testUser", "Test User");
    }

    // ==================
    // 캡슐 생성 테스트
    // ==================

    // 공개 캡슐 생성 테스트
    @Test
    void testPublicCapsuleCreate() {
        // given
        CapsuleCreateRequestDTO dto = new CapsuleCreateRequestDTO(
                1L, "nick", "title", "content", null,
                "white", "blue", "PUBLIC", "TIME",
                LocalDateTime.now(), null, "Seoul", "창원시 의창구",37.11, 127.22,
                100, 10
        );

        Capsule capsule = dto.toEntity();
        capsule.setMemberId(member);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(capsuleRepository.save(any(Capsule.class))).thenReturn(capsule);

        // when
        CapsuleCreateResponseDTO response = capsuleCreateService.publicCapsuleCreate(dto);

        // then
        assertNotNull(response);
        assertEquals(member.getMemberId(), response.memberId());
        assertEquals("title", response.title());
    }


    // 비공개 캡슐 생성 (url + 비밀번호 방식)
    @Test
    void testPrivateCapsulePassword() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, null, "1234", "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        String originalPassword = "1234";
        String hashedPassword = "hashedPw";

        Capsule capsule = dto.toEntity();
        capsule.setMemberId(member);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(phoneCrypto.hash(originalPassword)).thenReturn(hashedPassword);
        when(capsuleRepository.save(any(Capsule.class))).thenReturn(capsule);

        // when
        SecretCapsuleCreateResponseDTO response =
                capsuleCreateService.createPrivateCapsule(dto);

        // then
        assertNotNull(response);
        assertEquals("title", response.title());
        assertEquals(originalPassword, response.capPW()); // 반환은 원본 PW
    }

    @Test
    void testPrivateCapsulePhone_MemberRecipient() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "01000000000", null, "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(phoneCrypto.hash("01000000000"))
                .thenReturn("hashedPhone");

        when(memberRepository.existsByPhoneHash("hashedPhone"))
                .thenReturn(true); // 회원 수신자

        when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(recipientRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SecretCapsuleCreateResponseDTO response =
                capsuleCreateService.createPrivateCapsule(dto);

        // then
        assertNotNull(response);
        assertNull(response.capPW()); // 회원 → 비밀번호 없음
        assertEquals("title", response.title());
    }



    // 비공개 캡슐 생성 (전화번호 방식 - 비회원 수신자)
    @Test
    void testPrivateCapsulePhone_NonMemberRecipient() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "01000000000", null, "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // 해시값 가짜로 생성
        given(phoneCrypto.hash("01000000000"))
                .willReturn("hashedPhone");

        // hashedPhone으로 찾기
        given(memberRepository.existsByPhoneHash("hashedPhone"))
                .willReturn(false); // 없으면 비회원

        when(phoneCrypto.hash(argThat(arg -> !arg.equals("01000000000"))))
                .thenReturn("hashedPW");

        when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SecretCapsuleCreateResponseDTO response =
                capsuleCreateService.createPrivateCapsule(dto);

        // then
        assertNotNull(response);
        assertNotNull(response.capPW()); // 비회원 → 비밀번호 존재
    }


    // memberId가 존재하지 않을 때 예외 테스트
    @Test
    void testPublicCapsuleCreate_MemberNotFound() {
        // given
        CapsuleCreateRequestDTO dto = new CapsuleCreateRequestDTO(
                99L, "nick", "title", "content", null,
                "white", "blue", "PUBLIC", "TIME",
                LocalDateTime.now(), null, "Seoul","창원시 의창구", 37.11, 127.22,
                100, 10
        );

        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.publicCapsuleCreate(dto)
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("비공개 캡슐 - 비회원 전화 번호 방식")
    void testPrivateCapsulePhone_MemberNotFound() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                99L, "01012341234", null, "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.createPrivateCapsule(dto)
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("나에게 보내는 캡슐 생성 성공")
    void capsuleToMe_success() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, null, null, "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        String encryptedPhone = "encryptedPhone123";
        String phoneHash = "hashedPhone456";

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(recipientRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SecretCapsuleCreateResponseDTO response =
                capsuleCreateService.capsuleToMe(dto, encryptedPhone, phoneHash);

        // then
        assertNotNull(response);
        assertEquals("title", response.title());
    }

    @Test
    @DisplayName("나에게 보내는 캡슐 - member 없음")
    void capsuleToMe_memberNotFound() {
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                99L, null, null, "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        String encryptedPhone = "encryptedPhone123";
        String phoneHash = "hashedPhone456";

        when(memberRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.capsuleToMe(dto, encryptedPhone, phoneHash)
        );
    }

    @Test
    @DisplayName("비공개 캡슐 생성 - 전화번호와 비밀번호 둘 다 없으면 예외 발생")
    void createPrivateCapsule_bothNull_throwsException() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, null, null, "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.createPrivateCapsule(dto)
        );

        assertEquals(ErrorCode.CAPSULE_NOT_CREATE, ex.getErrorCode());
    }

    @Test
    @DisplayName("비공개 캡슐 생성 - 전화번호와 비밀번호 둘 다 있으면 예외 발생")
    void createPrivateCapsule_bothProvided_throwsException() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "01000000000", "1234", "nick", "receiver","title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), null, "Seoul","창원시 의창구",
                37.11, 127.22, 300, "red", "white", 10
        );

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.createPrivateCapsule(dto)
        );

        assertEquals(ErrorCode.CAPSULE_NOT_CREATE, ex.getErrorCode());
    }

    // =======================
    // 캡슐 수정 테스트
    // =======================


    @Test
    @DisplayName("캡슐 수정 성공")
    void updateCapsule_success() {
        // given
        Long capsuleId = 1L;

        Member member = Member.testMember(10L, "tester", "테스터");

        Capsule capsule = Capsule.builder()
                .capsuleId(capsuleId)
                .memberId(member)
                .title("old title")
                .content("old content")
                .build();

        CapsuleUpdateRequestDTO dto =
                new CapsuleUpdateRequestDTO("new title", "new content");

        // 캡슐열람 로그 없음 → 수정 가능

        Mockito.when(capsuleRepository.findById(capsuleId))
                .thenReturn(Optional.of(capsule));

        Mockito.when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CapsuleUpdateResponseDTO response =
                capsuleCreateService.updateCapsule(capsuleId, dto);

        // then
        assertEquals("new title", response.updatedTitle());
        assertEquals("new content", response.updatedContent());
    }


    @Test
    @DisplayName("캡슐 열람 기록이 존재하면 수정 불가")
    void updateCapsule_fail_dueToOpenedCapsule() {
        // given
        Long capsuleId = 1L;

        when(capsuleRepository.findCurrentViewCountByCapsuleId(capsuleId))
                .thenReturn(1);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.updateCapsule(
                        capsuleId,
                        new CapsuleUpdateRequestDTO("title", "content")
                )
        );

        assertEquals(ErrorCode.CAPSULE_NOT_UPDATE, ex.getErrorCode());
    }



    @Test
    @DisplayName("캡슐을 찾을 수 없으면 예외 발생")
    void updateCapsule_fail_capsuleNotFound() {
        // given
        Long capsuleId = 100L;

        Mockito.when(capsuleRepository.findById(capsuleId))
                .thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.updateCapsule(
                        capsuleId,
                        new CapsuleUpdateRequestDTO("title", "content")
                )
        );

        assertEquals(ErrorCode.CAPSULE_NOT_FOUND, ex.getErrorCode());
    }


    @Test
    @DisplayName("title만 수정되는 경우")
    void updateCapsule_onlyTitle() {
        // given
        Long capsuleId = 1L;

        Member member = Member.testMember(10L, "tester", "테스터");

        Capsule capsule = Capsule.builder()
                .capsuleId(capsuleId)
                .memberId(member)
                .title("old title")
                .content("old content")
                .build();

        CapsuleUpdateRequestDTO dto =
                new CapsuleUpdateRequestDTO("new title", null);


        Mockito.when(capsuleRepository.findById(capsuleId))
                .thenReturn(Optional.of(capsule));

        Mockito.when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CapsuleUpdateResponseDTO response =
                capsuleCreateService.updateCapsule(capsuleId, dto);

        // then
        assertEquals("new title", response.updatedTitle());
        assertEquals("old content", response.updatedContent());
    }

    @Test
    @DisplayName("content만 수정되는 경우")
    void updateCapsule_onlyContent() {
        // given
        Long capsuleId = 1L;

        Member member = Member.testMember(10L, "tester", "테스터");

        Capsule capsule = Capsule.builder()
                .capsuleId(capsuleId)
                .memberId(member)
                .title("old title")
                .content("old content")
                .build();

        CapsuleUpdateRequestDTO dto =
                new CapsuleUpdateRequestDTO(null, "new content");

        Mockito.when(capsuleRepository.findById(capsuleId))
                .thenReturn(Optional.of(capsule));

        Mockito.when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CapsuleUpdateResponseDTO response =
                capsuleCreateService.updateCapsule(capsuleId, dto);

        // then
        assertEquals("old title", response.updatedTitle());
        assertEquals("new content", response.updatedContent());
    }

    // =========================
    // 캡슐 삭제 테스트
    // ==========================

    @Test
    @DisplayName("수신자 캡슐 삭제 성공 - PRIVATE")
    void receiverDelete_private_success() {
        // given
        Long capsuleId = 1L;
        String phoneHash = "hashed-phone";

        CapsuleRecipient privateRecipient = mock(CapsuleRecipient.class);

        given(recipientRepository
                .findByCapsuleId_CapsuleIdAndRecipientPhoneHash(capsuleId, phoneHash))
                .willReturn(Optional.of(privateRecipient));

        // when
        CapsuleDeleteResponseDTO response =
                capsuleCreateService.receiverDelete(capsuleId, phoneHash);

        // then
        verify(privateRecipient).markDeleted();
        verify(recipientRepository).save(privateRecipient);

        // PUBLIC 조회는 타지 않아야 함
        verify(publicRecipientRepository, never())
                .findByCapsuleIdAndPhoneHash(any(), any());

        assertThat(response.capsuleId()).isEqualTo(capsuleId);
        assertThat(response.message()).contains("삭제");
    }


    @Test
    @DisplayName("수신자 캡슐 삭제 성공 - PUBLIC")
    void receiverDelete_public_success() {
        // given
        Long capsuleId = 1L;
        String phoneHash = "hashed-phone";

        // PRIVATE 없음
        given(recipientRepository
                .findByCapsuleId_CapsuleIdAndRecipientPhoneHash(capsuleId, phoneHash))
                .willReturn(Optional.empty());

        PublicCapsuleRecipient publicRecipient = mock(PublicCapsuleRecipient.class);

        given(publicRecipientRepository
                .findByCapsuleIdAndPhoneHash(capsuleId, phoneHash))
                .willReturn(Optional.of(publicRecipient));

        // when
        CapsuleDeleteResponseDTO response =
                capsuleCreateService.receiverDelete(capsuleId, phoneHash);

        // then
        verify(publicRecipient).markDeleted();
        verify(publicRecipientRepository).save(publicRecipient);

        assertThat(response.capsuleId()).isEqualTo(capsuleId);
        assertThat(response.message()).contains("삭제");
    }


    @Test
    @DisplayName("수신자 캡슐이 PRIVATE / PUBLIC 모두 없으면 예외 발생")
    void receiverDelete_notFound() {
        // given
        Long capsuleId = 1L;
        String phoneHash = "hashed-phone";

        given(recipientRepository
                .findByCapsuleId_CapsuleIdAndRecipientPhoneHash(capsuleId, phoneHash))
                .willReturn(Optional.empty());

        given(publicRecipientRepository
                .findByCapsuleIdAndPhoneHash(capsuleId, phoneHash))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                capsuleCreateService.receiverDelete(capsuleId, phoneHash)
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CAPSULE_NOT_FOUND);
    }



    @Test
    @DisplayName("발신자 캡슐 삭제 성공")
    void senderDelete_success() {
        // given
        Long memberId = 10L;
        Long capsuleId = 1L;

        Capsule capsule = mock(Capsule.class);

        given(capsuleRepository
                .findByCapsuleIdAndMemberId_MemberId(capsuleId, memberId))
                .willReturn(Optional.of(capsule));

        // when
        CapsuleDeleteResponseDTO response =
                capsuleCreateService.senderDelete(capsuleId, memberId);

        // then
        verify(capsule).markDeleted();
        verify(capsule).setIsDeleted(1);
        verify(capsuleRepository).save(capsule);

        assertThat(response.capsuleId()).isEqualTo(capsuleId);
        assertThat(response.message()).contains("삭제");
    }

    @Test
    @DisplayName("발신자 캡슐이 없으면 예외 발생")
    void senderDelete_notFound() {
        // given
        Long memberId = 10L;
        Long capsuleId = 1L;

        given(capsuleRepository
                .findByCapsuleIdAndMemberId_MemberId(memberId, capsuleId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                capsuleCreateService.senderDelete(memberId, capsuleId)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.CAPSULE_NOT_FOUND.getMessage());
    }
}
