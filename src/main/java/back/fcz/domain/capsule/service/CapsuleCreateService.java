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
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final CapsuleRecipientRepository recipientRepository;
    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final PublicCapsuleRecipientRepository publicRecipientRepository;

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

        // TODO: 캡슐 이미지 추가 하실 때 여기서 하시면 됩니다.
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

        // 닉네임 null일 때 방지, "" 닉네임은 가능
        if(secretCapsule.getReceiverNickname() == null){
            throw new BusinessException(ErrorCode.RECEIVERNICKNAME_IS_REQUIRED);
        }

        // TODO: 캡슐 이미지 추가 하실 때 여기서 하시면 됩니다.
        secretCapsule.setUuid(setUUID());
        secretCapsule.setCapPassword(phoneCrypto.hash(password)); // 사용자가 지정한 비밀번호 저장
        secretCapsule.setMemberId(member);

        Capsule saved = capsuleRepository.save(secretCapsule);

        String url  = domain + "/" +saved.getUuid();

        return SecretCapsuleCreateResponseDTO.from(saved, url, password);
    }

    // 비공개 캡슐 생성 - 전화 번호 조회
    public SecretCapsuleCreateResponseDTO privateCapsulePhone (SecretCapsuleCreateRequestDTO capsuleCreate, String receiveTel){

        Capsule capsule = capsuleCreate.toEntity();
        capsule.setUuid(setUUID());

        // 닉네임 null일 때 방지, "" 닉네임은 가능
        if(capsule.getReceiverNickname() == null){
            throw new BusinessException(ErrorCode.RECEIVERNICKNAME_IS_REQUIRED);
        }

        Member member = memberRepository.findById(capsuleCreate.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if(memberRepository.existsByPhoneHash(phoneCrypto.hash(receiveTel))){ // 회원

            // TODO: 캡슐 이미지 추가 하실 때 여기서 하시면 됩니다.
            capsule.setMemberId(member);
            capsule.setProtected(1);
            Capsule saved = capsuleRepository.save(capsule);

            CapsuleRecipient recipientRecord = CapsuleRecipient.builder()
                    .capsuleId(saved)
                    .recipientName(capsuleCreate.nickname())
                    .recipientPhone(receiveTel)
                    .recipientPhoneHash(phoneCrypto.hash(receiveTel))
                    .isSenderSelf(0)
                    .build();

            recipientRepository.save(recipientRecord);

            String url = domain + "/" + saved.getUuid();

            return SecretCapsuleCreateResponseDTO.from(saved, url, null);

        }else{ // 비회원

            String capsulePW = generatePassword(); // 생성한 비밀번호
            capsule.setCapPassword(phoneCrypto.hash(capsulePW));
            capsule.setMemberId(member);

            // TODO: 캡슐 이미지 추가 하실 때 여기서 하시면 됩니다.
            Capsule saved = capsuleRepository.save(capsule);

            String url = domain + "/" + saved.getUuid();

            return SecretCapsuleCreateResponseDTO.from(saved, url, capsulePW);
        }
    }

    // 비공개 캡슐 - 나에게 보내는 캡슐
    public SecretCapsuleCreateResponseDTO capsuleToMe(SecretCapsuleCreateRequestDTO requestDTO, String receiveTel){
        Capsule capsule = requestDTO.toEntity();

        // 닉네임 null일 때 방지, "" 닉네임은 가능
        if(capsule.getReceiverNickname() == null){
            throw new BusinessException(ErrorCode.RECEIVERNICKNAME_IS_REQUIRED);
        }

        Member member = memberRepository.findById(requestDTO.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 캡슐 설정
        // TODO: 캡슐 이미지 추가 하실 때 여기서 하시면 됩니다.
        capsule.setProtected(1);
        capsule.setUuid(setUUID());
        capsule.setMemberId(member);

        Capsule saved = capsuleRepository.save(capsule);

        // 수신자 테이블에 저장
        CapsuleRecipient recipientRecord = CapsuleRecipient.builder()
                .capsuleId(saved)
                .recipientName(requestDTO.nickname())
                .recipientPhone(receiveTel)
                .recipientPhoneHash(phoneCrypto.hash(receiveTel))
                .isSenderSelf(1)
                .build();

        recipientRepository.save(recipientRecord);

        return SecretCapsuleCreateResponseDTO.from(saved, null, null);
    }

    // 캡슐 수정
    public CapsuleUpdateResponseDTO updateCapsule(
            Long capsuleId,
            CapsuleUpdateRequestDTO updateDTO
    ){
        // 캡슐 조회수 확인
        if(capsuleRepository.findCurrentViewCountByCapsuleId(capsuleId) > 0) throw new BusinessException(ErrorCode.CAPSULE_NOT_UPDATE);

        // 수정 진행
        Capsule targetCapsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        if(updateDTO.title() != null){
            targetCapsule.setTitle(updateDTO.title());
        }

        if(updateDTO.content() != null){
            targetCapsule.setContent(updateDTO.content());
        }

        Capsule saved = capsuleRepository.save(targetCapsule);
        return CapsuleUpdateResponseDTO.from(saved);
    }

    // 캡슐 삭제
    // 수신자 삭제
    public CapsuleDeleteResponseDTO receiverDelete(
            Long capsuleId,
            String phoneHash
    ){
        // 수신자 캡슐 존재 확인
        Optional<CapsuleRecipient> privateOpt = recipientRepository.findByCapsuleId_CapsuleIdAndRecipientPhoneHash(capsuleId, phoneHash);

        if(privateOpt.isPresent()){
            CapsuleRecipient privateCapsule = privateOpt.get();

            // deletedAt 갱신
            privateCapsule.markDeleted();

            recipientRepository.save(privateCapsule);

            return new CapsuleDeleteResponseDTO(
                    capsuleId,
                    capsuleId + "번 캡슐이 삭제 되었습니다."
            );
        }

        Optional<PublicCapsuleRecipient> publicOps = publicRecipientRepository.findByCapsuleIdAndPhoneHash(capsuleId, phoneHash);

        if(publicOps.isPresent()){
            PublicCapsuleRecipient publicCapsule = publicOps.get();

            // deletedAt 갱신
            publicCapsule.markDeleted();

            publicRecipientRepository.save(publicCapsule);

            return new CapsuleDeleteResponseDTO(
                    capsuleId,
                    capsuleId + "번 캡슐이 삭제 되었습니다."
            );
        }else{
            throw new BusinessException(ErrorCode.CAPSULE_NOT_FOUND);
        }
    }

    // 발신자 삭제
     public CapsuleDeleteResponseDTO senderDelete(
             Long memberId,
             Long capsuleId
     ){

        // 발신자 캡슐 존재 확인
         Capsule capsule = capsuleRepository.findByCapsuleIdAndMemberId_MemberId(capsuleId, memberId)
                 .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

         // 삭제 내용 갱신
         capsule.markDeleted();
         capsule.setIsDeleted(1);

         capsuleRepository.save(capsule);

         return new CapsuleDeleteResponseDTO(
                 capsuleId,
                 capsuleId + "번 캡슐이 삭제 되었습니다."
         );
     }
}
