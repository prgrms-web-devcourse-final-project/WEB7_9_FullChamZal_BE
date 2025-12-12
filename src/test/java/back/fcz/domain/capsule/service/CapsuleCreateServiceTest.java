package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleUpdateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleCreateResponseDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleUpdateResponseDTO;
import back.fcz.domain.capsule.DTO.response.SecretCapsuleCreateResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapsuleCreateServiceTest {

    @Mock
    CapsuleRepository capsuleRepository;
    @Mock
    CapsuleRecipientRepository recipientRepository;
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
                LocalDateTime.now(), "Seoul", 37.11, 127.22,
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
                1L, "nick", "title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), "Seoul",
                37.11, 127.22, 300, "red", "white", 10
        );

        String originalPassword = "1234";
        String encryptedPassword = "encryptedPw";

        Capsule capsule = dto.toEntity();
        capsule.setMemberId(member);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(phoneCrypto.encrypt(originalPassword)).thenReturn(encryptedPassword);
        when(capsuleRepository.save(any(Capsule.class))).thenReturn(capsule);

        // when
        SecretCapsuleCreateResponseDTO response =
                capsuleCreateService.privateCapsulePassword(dto, originalPassword);

        // then
        assertNotNull(response);
        assertEquals("title", response.title());
        assertEquals(originalPassword, response.capPW()); // 반환은 원본 PW
    }

    @Test
    void testPrivateCapsulePhone_MemberRecipient() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "nick", "title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), "Seoul",
                37.11, 127.22, 300, "red", "white", 10
        );

        Member recipient = Member.testMember(2L, "reciever", "recieverName");

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(phoneCrypto.hash("01000000000"))
                .thenReturn("hashedPhone");

        when(memberRepository.findByPhoneHash("hashedPhone"))
                .thenReturn(Optional.of(recipient));

        when(memberRepository.existsByPhoneHash("hashedPhone"))
                .thenReturn(true);

        when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(recipientRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SecretCapsuleCreateResponseDTO response =
                capsuleCreateService.privateCapsulePhone(dto, "01000000000");

        // then
        assertNotNull(response);
        assertNull(response.capPW());
        assertEquals("title", response.title());
    }


    // 비공개 캡슐 생성 (전화번호 방식 - 비회원 수신자)
    @Test
    void testPrivateCapsulePhone_NonMemberRecipient() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                1L, "nick", "title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), "Seoul",
                37.11, 127.22, 300, "red", "white", 10
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // 해시값 가짜로 생성
        when(phoneCrypto.hash(anyString()))
                .thenReturn("hashedPhone");

        // hashedPhone 으로 찾으면 회원 없음 → 비회원 분기
        when(memberRepository.findByPhoneHash("hashedPhone"))
                .thenReturn(Optional.empty());

        when(phoneCrypto.encrypt(anyString()))
                .thenReturn("encryptedPw");

        when(capsuleRepository.save(any(Capsule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SecretCapsuleCreateResponseDTO response =
                capsuleCreateService.privateCapsulePhone(dto, "01000000000");

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
                LocalDateTime.now(), "Seoul", 37.11, 127.22,
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
    void testPrivateCapsulePhone_MemberNotFound() {
        // given
        SecretCapsuleCreateRequestDTO dto = new SecretCapsuleCreateRequestDTO(
                99L, "nick", "title", "content", "PRIVATE",
                "TIME", LocalDateTime.now(), "Seoul",
                37.11, 127.22, 300, "red", "white", 10
        );

        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> capsuleCreateService.privateCapsulePhone(dto, "01012341234")
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.getErrorCode());
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
        Mockito.when(capsuleOpenLogRepository.findByCapsuleId_CapsuleId(capsuleId))
                .thenReturn(Optional.empty());

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

        // 열람 로그가 존재한다고 가정
        Mockito.when(capsuleOpenLogRepository.findByCapsuleId_CapsuleId(capsuleId))
                .thenReturn(Optional.of(new CapsuleOpenLog()));

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

        Mockito.when(capsuleOpenLogRepository.findByCapsuleId_CapsuleId(capsuleId))
                .thenReturn(Optional.empty());

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

        Mockito.when(capsuleOpenLogRepository.findByCapsuleId_CapsuleId(capsuleId))
                .thenReturn(Optional.empty());

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

        Mockito.when(capsuleOpenLogRepository.findByCapsuleId_CapsuleId(capsuleId))
                .thenReturn(Optional.empty());

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
}
