package back.fcz.domain.member.service;

import back.fcz.domain.member.dto.request.MemberUpdateRequest;
import back.fcz.domain.member.dto.request.PasswordVerifyRequest;
import back.fcz.domain.member.dto.response.MemberDetailResponse;
import back.fcz.domain.member.dto.response.MemberInfoResponse;
import back.fcz.domain.member.dto.response.MemberUpdateResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.entity.NicknameHistory;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.repository.NicknameHistoryRepository;
import back.fcz.domain.member.util.PhoneMaskingUtil;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final PhoneCrypto phoneCrypto;
    private final NicknameHistoryRepository nicknameHistoryRepository;

    private static final int NICKNAME_CHANGE_COOLDOWN_DAYS = 90;

    public void verifyPassword(InServerMemberResponse user, PasswordVerifyRequest request) {
        Member member = memberRepository.findById(user.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }

    public MemberInfoResponse getMe(InServerMemberResponse user) {
        Member member = memberRepository.findById(user.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String decryptedPhone = phoneCrypto.decrypt(member.getPhoneNumber());
        String maskedPhone = PhoneMaskingUtil.mask(decryptedPhone);

        return MemberInfoResponse.of(member, maskedPhone);
    }

    public MemberDetailResponse getDetailMe(InServerMemberResponse user) {
        Member member = memberRepository.findById(user.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String decryptedPhone = phoneCrypto.decrypt(member.getPhoneNumber());

        return MemberDetailResponse.of(member, decryptedPhone);
    }

    @Transactional
    public MemberUpdateResponse updateMember(InServerMemberResponse user, MemberUpdateRequest request) {
        validateUpdateRequest(request);

        Member member = memberRepository.findById(user.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<String> updatedFields = new ArrayList<>();
        LocalDateTime nextNicknameChangeDate = null;

        if(request.hasNicknameChange()) {
            nextNicknameChangeDate = updateNickname(member, request.nickname());
            updatedFields.add("닉네임");
        }

        if (request.hasPasswordChange()) {
            updatePassword(member, request.currentPassword(), request.newPassword());
            updatedFields.add("비밀번호");
        }

        if (request.hasPhoneChange()) {
            updatePhoneNumber(member, request.phoneNumber());
            updatedFields.add("전화번호");
        }

        return MemberUpdateResponse.of(updatedFields, nextNicknameChangeDate);
    }

    @Transactional
    public void delete(InServerMemberResponse user) {
        Member member = memberRepository.findById(user.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.markDeleted();
        member.updateStatus(MemberStatus.EXIT);

        log.info("회원 탈퇴 처리 완료 - memberId: {}, userId: {}, deletedAt: {}",
                member.getMemberId(), member.getUserId(), member.getDeletedAt());
    }

    private void validateUpdateRequest(MemberUpdateRequest request) {
        // 변경 요청이 없으면 예외
        if(!request.hasAnyChange()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 비밀번호 변경 시 현재 비밀번호 필수
        if(request.hasPasswordChange()) {
            if(request.currentPassword() == null || request.currentPassword().isBlank()){
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }

    private LocalDateTime updateNickname(Member member, String newNickname) {
        if (memberRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        LocalDateTime lastChangeDate = member.getNicknameChangedAt();
        if (lastChangeDate != null) {
            LocalDateTime nextChangeDate = lastChangeDate.plusDays(NICKNAME_CHANGE_COOLDOWN_DAYS);
            if (LocalDateTime.now().isBefore(nextChangeDate)) {
                log.warn("닉네임 변경 제한 기간 - memberId: {}, nextChangeDate: {}",
                        member.getMemberId(), nextChangeDate);
                throw new BusinessException(ErrorCode.NICKNAME_CHANGE_TOO_SOON);
            }
        }

        String oldNickname = member.getNickname();
        NicknameHistory history = NicknameHistory.create(
                member.getMemberId(),
                oldNickname,
                newNickname
        );
        nicknameHistoryRepository.save(history);

        member.updateNickname(newNickname);

        LocalDateTime nextChangeDate = LocalDateTime.now().plusDays(NICKNAME_CHANGE_COOLDOWN_DAYS);
        log.info("닉네임 변경 완료 - memberId: {}, old: {}, new: {}, nextChangeDate: {}",
                member.getMemberId(), oldNickname, newNickname, nextChangeDate);

        return nextChangeDate;
    }

    private void updatePassword(Member member, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
            log.warn("비밀번호 변경 실패: 현재 비밀번호 불일치 - memberId: {}",
                    member.getMemberId());
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);

        member.updatePassword(newPasswordHash);
    }

    private void updatePhoneNumber(Member member, String newPhoneNumber) {
        // TODO: 번호 인증 확인

        String newPhoneHash = phoneCrypto.hash(newPhoneNumber);
        if (memberRepository.existsByPhoneHash(newPhoneHash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONENUM);
        }

        String encryptedPhone = phoneCrypto.encrypt(newPhoneNumber);

        member.updatePhoneNumber(encryptedPhone, newPhoneHash);
    }
}
