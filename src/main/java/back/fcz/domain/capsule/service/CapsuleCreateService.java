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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    String domain = "http://localhost:8080/api/v1/capsule/"; // 임시로 엔드포인트 사용

    // UUID 생성
    public String setUUID(){
        return UUID.randomUUID().toString();
    }

    // 비밀번호 생성 - 4자리 숫자 번호
    private String generatePassword() {
        Random random = new Random();
        int number = random.nextInt(9000) + 1000;
        return String.valueOf(number);
    }

    // 공개 캡슐 생성
    public CapsuleCreateResponseDTO publicCapsuleCreate(CapsuleCreateRequestDTO capsuleCreate){
        Capsule capsule = capsuleCreate.toEntity();
        capsule.setUuid(setUUID());
        Capsule saved = capsuleRepository.save(capsule);

        return CapsuleCreateResponseDTO.from(saved);
    }

    // 비공개 캡슐 생성 - URL + 비밀번호 조회
    public SecretCapsuleCreateResponseDTO selfCreateCapsule(SecretCapsuleCreateRequestDTO capsuleCreate, String password){

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new RuntimeException("Member not found")); // 에러코드 작성

        Capsule secretCapsule = capsuleCreate.toEntity();

        secretCapsule.setUuid(setUUID());
        secretCapsule.setCapPassword(password);
        secretCapsule.setMemberId(member);

        Capsule saved = capsuleRepository.save(secretCapsule);

        String url  = domain + secretCapsule.getUuid();

        return SecretCapsuleCreateResponseDTO.from(saved, url);
    }

    // 비공개 캡슐 생성 - 전화 번호 조회
    public SecretCapsuleCreateResponseDTO creatCapsule(SecretCapsuleCreateRequestDTO capsuleCreate, String receiveTel){

        // 캡슐 생성
        Capsule capsule = capsuleCreate.toEntity();
        capsule.setUuid(setUUID());

        // 전화 번호로 멤버 조회
        Member member = memberRepository.findByphoneNumber(receiveTel);

        if(member != null){ // 회원
            // 캡슐 저장
            capsule.setMemberId(member);
            capsule.setProtected(1);
            Capsule saved = capsuleRepository.save(capsule);

            // URL 생성
            String url = domain + capsule.getUuid();


            // 수신자 테이블 저장
            CapsuleRecipient recipient = CapsuleRecipient.builder()
                    .capsuleId(saved)
                    .recipientName(capsuleCreate.nickName())
                    .recipientPhone(capsuleCreate.phoneNum())
                    .recipientPhoneHash(phoneCrypto.hash(capsuleCreate.phoneNum())) // 해시 생성 코드 사용
                    .isSenderSelf(false)
                    .build();

            recipientRepository.save(recipient);

            return SecretCapsuleCreateResponseDTO.from(saved, url);

        }else{ // 비회원
            // 캡슐 비밀번호 생성
            String capsulePW = generatePassword();
            capsule.setCapPassword(capsulePW);

            // URL 생성
            String url = domain + capsule.getUuid();

            // 캡슐 테이블에 저장
            Capsule saved = capsuleRepository.save(capsule);

            return SecretCapsuleCreateResponseDTO.from(saved, url);
        }
    }

    // 캡슐 수정

    // 캡슐 삭제
}
