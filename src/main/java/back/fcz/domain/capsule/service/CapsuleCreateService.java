package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.request.CapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.request.SecretCapsuleCreateRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleCreateResponseDTO;
import back.fcz.domain.capsule.DTO.response.SecretCapsuleCreateResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CapsuleCreateService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository recipientRepository;
    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;

    // url 도메인
    @Value("${cors.capsule-domain}")
    private String domain;

    // UUID 생성
    public String setUUID(){
        return UUID.randomUUID().toString();
    }

    // 캡슐 비밀번호 생성 - 4자리 숫자 번호
    private String generatePassword() {
        Random random = new Random();
        int number = random.nextInt(9000) + 1000;
        return String.valueOf(number);
    }

    // 공개 캡슐 생성
    public CapsuleCreateResponseDTO publicCapsuleCreate(CapsuleCreateRequestDTO capsuleCreate){
        Capsule capsule = capsuleCreate.toEntity();

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        capsule.setMemberId(member);
        capsule.setUuid(setUUID());
        Capsule saved = capsuleRepository.save(capsule);

        return CapsuleCreateResponseDTO.from(saved);
    }

    // 비공개 캡슐 생성 - URL + 비밀번호 조회
    public SecretCapsuleCreateResponseDTO privateCapsulePassword (SecretCapsuleCreateRequestDTO capsuleCreate, String password){

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Capsule secretCapsule = capsuleCreate.toEntity();

        secretCapsule.setUuid(setUUID());
        secretCapsule.setCapPassword(phoneCrypto.encrypt(password)); // 사용자가 지정한 비밀번호 저장
        secretCapsule.setMemberId(member);

        Capsule saved = capsuleRepository.save(secretCapsule);

        String url  = domain + saved.getUuid();

        return SecretCapsuleCreateResponseDTO.from(saved, url, password);
    }

    // 비공개 캡슐 생성 - 전화 번호 조회
    public SecretCapsuleCreateResponseDTO privateCapsulePhone (SecretCapsuleCreateRequestDTO capsuleCreate, String receiveTel){

        Capsule capsule = capsuleCreate.toEntity();
        capsule.setUuid(setUUID());

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Optional<Member> recipient = memberRepository.findByPhoneHash(phoneCrypto.hash(receiveTel));

        if(memberRepository.existsByPhoneHash(phoneCrypto.hash(receiveTel))){ // 회원
            capsule.setMemberId(member);
            capsule.setProtected(1);
            Capsule saved = capsuleRepository.save(capsule);

            CapsuleRecipient recipientRecord = CapsuleRecipient.builder()
                    .capsuleId(saved)
                    .recipientName(capsuleCreate.nickName())
                    .recipientPhone(receiveTel)
                    .recipientPhoneHash(phoneCrypto.hash(receiveTel))
                    .isSenderSelf(false)
                    .build();

            recipientRepository.save(recipientRecord);

            String url = domain + saved.getUuid();

            return SecretCapsuleCreateResponseDTO.from(saved, url, null);

        }else{ // 비회원
            String capsulePW = generatePassword(); // 생성한 비밀번호
            capsule.setCapPassword(phoneCrypto.encrypt(capsulePW));
            capsule.setMemberId(member);

            Capsule saved = capsuleRepository.save(capsule);

            String url = domain + saved.getUuid();

            return SecretCapsuleCreateResponseDTO.from(saved, url, capsulePW);
        }
    }

    // 캡슐 수정

    // 캡슐 삭제
}
