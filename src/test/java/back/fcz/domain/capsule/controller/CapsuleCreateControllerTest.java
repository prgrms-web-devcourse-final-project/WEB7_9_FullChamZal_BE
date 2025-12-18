package back.fcz.domain.capsule.controller;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.request.CapsuleUpdateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CapsuleCreateControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CapsuleRepository capsuleRepository;

    @Autowired
    CapsuleRecipientRepository capsuleRecipientRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PhoneCrypto phoneCrypto;

    // 인증 컨텍스트만 Mock
    @MockitoBean
    CurrentUserContext currentUserContext;

    // =========================
    // 공개 캡슐 생성
    // =========================
    @Test
    @DisplayName("통합 테스트 - 공개 캡슐 생성 성공")
    void createPublicCapsule_success() throws Exception {

        Member member = memberRepository.save(
                Member.builder()
                        .userId("testuser-public")
                        .name("홍길동")
                        .nickname("테스터")
                        .passwordHash("pw")
                        .phoneHash("hash")
                        .phoneNumber("encrypted")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        CapsuleCreateRequestDTO requestDTO =
                new CapsuleCreateRequestDTO(
                        member.getMemberId(),
                        "nick",
                        "공개 캡슐",
                        "내용",
                        null,
                        "white",
                        "navy",
                        "PUBLIC",
                        "TIME",
                        LocalDateTime.now().plusDays(1),
                        null,
                        "Seoul",
                        "창원시 의창구",
                        37.5,
                        127.0,
                        100,
                        5
                );

        mockMvc.perform(post("/api/v1/capsule/create/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("공개 캡슐"))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));


        assertEquals(1, capsuleRepository.count());
    }

    // ===========================
    // 비공개 캡슐
    // ==========================

    @Test
    @DisplayName("통합 테스트 - 비공개 캡슐 생성 (URL + 비밀번호)")
    void createPrivateCapsule_password_success() throws Exception {

        Member member = memberRepository.save(
                Member.builder()
                        .userId("testuser-private")
                        .name("홍길동")
                        .nickname("테스터")
                        .passwordHash("pw")
                        .phoneHash("hash")
                        .phoneNumber("encrypted")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        SecretCapsuleCreateRequestDTO requestDTO =
                new SecretCapsuleCreateRequestDTO(
                        member.getMemberId(),
                        "senderNick",
                        "receiver",
                        "비공개 캡슐",
                        "내용",
                        "PRIVATE",
                        "TIME",
                        LocalDateTime.now().plusDays(1),
                        null,
                        "Seoul",
                        "창원시 의창구",
                        37.5,
                        127.0,
                        100,
                        "red",
                        "white",
                        5
                );

        mockMvc.perform(post("/api/v1/capsule/create/private")
                        .param("capsulePassword", "1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("비공개 캡슐"))
                .andExpect(jsonPath("$.data.capPW").value("1234"))
                .andExpect(jsonPath("$.data.url").exists());

        assertEquals(1, capsuleRepository.count());
    }

    @Test
    @DisplayName("통합 테스트 - 비공개 캡슐 생성 (전화번호 / 회원)")
    void createPrivateCapsule_phone_member_success() throws Exception {

        Member sender = memberRepository.save(
                Member.builder()
                        .userId("sender")
                        .name("보낸이")
                        .nickname("보낸이닉")
                        .passwordHash("pw")
                        .phoneHash("hash1")
                        .phoneNumber("encrypted1")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );


        Member receiver = memberRepository.save(
                Member.builder()
                        .userId("receiver")
                        .name("받는이")
                        .nickname("받는이닉")
                        .passwordHash("pw")
                        .phoneHash(phoneCrypto.hash("01000000000"))
                        .phoneNumber("encrypted2")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        SecretCapsuleCreateRequestDTO requestDTO =
                new SecretCapsuleCreateRequestDTO(
                        sender.getMemberId(),
                        "senderNick",
                        "receiver",
                        "비공개 캡슐",
                        "내용",
                        "PRIVATE",
                        "TIME",
                        LocalDateTime.now().plusDays(1),
                        null,
                        "Seoul",
                        "창원시 의창구",
                        37.5,
                        127.0,
                        100,
                        "red",
                        "white",
                        5
                );

        mockMvc.perform(post("/api/v1/capsule/create/private")
                        .param("phoneNum", "01000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capPW").doesNotExist());

        assertEquals(1, capsuleRepository.count());
        assertEquals(1, capsuleRecipientRepository.count());
    }

    @Test
    @DisplayName("비공개 캡슐 생성 - receiverNickname 없으면 실패")
    void createPrivateCapsule_receiverNickname_null() throws Exception {

        Member member = memberRepository.save(
                Member.builder()
                        .userId("testuser")
                        .name("홍길동")
                        .nickname("테스터")
                        .passwordHash("pw")
                        .phoneHash("hash")
                        .phoneNumber("encrypted")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        SecretCapsuleCreateRequestDTO requestDTO =
                new SecretCapsuleCreateRequestDTO(
                        member.getMemberId(),
                        "senderNick",
                        null,
                        "비공개 캡슐",
                        "내용",
                        "PRIVATE",
                        "TIME",
                        LocalDateTime.now().plusDays(1),
                        null,
                        "Seoul",
                        "창원시 의창구",
                        37.5,
                        127.0,
                        100,
                        "red",
                        "white",
                        5
                );

        mockMvc.perform(post("/api/v1/capsule/create/private")
                        .param("capsulePassword", "1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest()); // 또는 GlobalExceptionHandler 기준 코드
    }

    // =========================
    // 비공개 캡슐 - 나에게 보내기
    // =========================
    @Test
    @DisplayName("통합 테스트 - 나에게 보내는 캡슐 생성 성공")
    void createToMeCapsule_success() throws Exception {

        Member member = memberRepository.save(
                Member.builder()
                        .userId("testuser-me")
                        .name("홍길동")
                        .nickname("테스터")
                        .passwordHash("pw")
                        .phoneHash("hash123")
                        .phoneNumber("encrypted456")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        SecretCapsuleCreateRequestDTO requestDTO =
                new SecretCapsuleCreateRequestDTO(
                        member.getMemberId(),
                        null,
                        null,
                        "senderNick",
                        "receiver",
                        "나에게 보내는 캡슐",
                        "내년에는 행복하자!",
                        "PRIVATE",
                        "TIME",
                        LocalDateTime.of(2025, 12, 31, 23, 59),
                        null,
                        "Seoul Station",
                        "창원시 의창구",
                        37.554722,
                        126.970833,
                        300,
                        "navy",
                        "white",
                        5
                );

        InServerMemberResponse mockUserResponse = new InServerMemberResponse(
                member.getMemberId(),
                member.getUserId(),
                member.getName(),
                member.getNickname(),
                "encrypted456",
                "hash123",
                member.getRole()
        );

        when(currentUserContext.getCurrentUser()).thenReturn(mockUserResponse);

        mockMvc.perform(post("/api/v1/capsule/create/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("나에게 보내는 캡슐"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));

        assertEquals(1, capsuleRepository.count());
        assertEquals(1, capsuleRecipientRepository.count());

        Capsule capsule = capsuleRepository.findAll().get(0);
        CapsuleRecipient recipient = capsuleRecipientRepository.findAll().get(0);

        assertEquals(member.getMemberId(), capsule.getMemberId().getMemberId());
        assertEquals(1, recipient.getIsSenderSelf());
        assertEquals(1, capsule.getIsProtected());
        assertNotNull(capsule.getUuid());

        assertEquals("encrypted456", recipient.getRecipientPhone());
        assertEquals("hash123", recipient.getRecipientPhoneHash());
    }

    // =========================
    // 캡슐 수정
    // =========================
    @Test
    @DisplayName("통합 테스트 - 캡슐 수정 성공")
    void updateCapsule_success() throws Exception {

        Member member = memberRepository.save(
                Member.builder()
                        .userId("testuser-update")
                        .name("홍길동")
                        .nickname("테스터")
                        .passwordHash("pw")
                        .phoneHash("hash")
                        .phoneNumber("encrypted")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        Capsule capsule = capsuleRepository.save(
                Capsule.builder()
                        .memberId(member)
                        .nickname(member.getNickname())
                        .title("old title")
                        .content("old content")
                        .visibility("PUBLIC")
                        .unlockType("TIME")
                        .capsuleColor("white")
                        .capsulePackingColor("navy")
                        .maxViewCount(5)
                        .locationRadiusM(100)
                        .uuid(UUID.randomUUID().toString())
                        .build()
        );

        CapsuleUpdateRequestDTO requestDTO =
                new CapsuleUpdateRequestDTO("new title", "new content");

        mockMvc.perform(put("/api/v1/capsule/update")
                        .param("capsuleId", capsule.getCapsuleId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedTitle").value("new title"))
                .andExpect(jsonPath("$.data.updatedContent").value("new content"));

    }

    // =========================
    // 캡슐 삭제 - 발신자
    // =========================
    @Test
    @DisplayName("통합 테스트 - 발신자 캡슐 삭제 성공")
    void senderDelete_success() throws Exception {

        Member member = memberRepository.save(
                Member.builder()
                        .userId("testuser-delete")
                        .name("홍길동")
                        .nickname("테스터")
                        .passwordHash("pw")
                        .phoneHash("hash")
                        .phoneNumber("encrypted")
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        Capsule capsule = capsuleRepository.save(
                Capsule.builder()
                        .memberId(member)
                        .nickname(member.getNickname())
                        .title("삭제할 캡슐")
                        .content("content")
                        .visibility("PUBLIC")
                        .unlockType("TIME")
                        .capsuleColor("white")
                        .capsulePackingColor("navy")
                        .maxViewCount(5)
                        .locationRadiusM(100)
                        .uuid(UUID.randomUUID().toString())
                        .build()
        );

        when(currentUserContext.getCurrentUser())
                .thenReturn(InServerMemberResponse.from(member));

        mockMvc.perform(delete("/api/v1/capsule/delete/sender")
                        .param("capsuleId", capsule.getCapsuleId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capsuleId").value(capsule.getCapsuleId()));

    }
}
