package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.DTO.response.DailyUnlockedCapsuleResponse;
import back.fcz.domain.capsule.DTO.response.MonthlyCapsuleStat;
import back.fcz.domain.capsule.DTO.response.UnlockedCapsuleResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleDashBoardService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final MemberRepository memberRepository;

    // 사용자가 전송한 캡슐 목록 조회
    public Page<CapsuleDashBoardResponse> readSendCapsuleList(Long memberId, Pageable pageable) {
        Page<Capsule> capsules = capsuleRepository.findActiveCapsulesByMemberId(memberId, pageable);
        return capsules.map(capsule -> {
            return new CapsuleDashBoardResponse(capsule);
        });
    }

    // 사용자가 수신한 캡슐 목록 조회
    public Page<CapsuleDashBoardResponse> readReceiveCapsuleList(Long memberId, Pageable pageable) {
        String phoneHash = memberRepository.findById(memberId).orElseThrow(() ->
                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).getPhoneHash();  // 사용자의 해시된 폰 번호

        // 수신자 테이블에서 phoneHash를 가지는 수신자 목록 조회
        Page<CapsuleRecipient> recipients = capsuleRecipientRepository.
                findAllByRecipientPhoneHashWithCapsule(phoneHash, pageable);

        // 수신자가 받은 캡슐 중, 수신자가 삭제하지 않은 캡슐만 조회
        return recipients.map(recipient -> {
            Capsule capsule = recipient.getCapsuleId();
            return new CapsuleDashBoardResponse(capsule);
        });
    }


    public List<MonthlyCapsuleStat> readYearlyCapsule(Long memberId, int year) {
        String phoneHash = memberRepository.findById(memberId).orElseThrow(() ->
                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).getPhoneHash();  // 사용자의 해시된 폰 번호

        // 1. 1월~12월까지 0으로 초기화된 리스트 생성
        List<MonthlyCapsuleStat> stats = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            stats.add(new MonthlyCapsuleStat(i + "월", 0, 0));
        }

        // 2. DB 데이터 가져오기 (List<Object[]>)
        List<Object[]> sendData = capsuleRepository.countMonthlySendCapsules(memberId, year);
        List<Object[]> receiveData = capsuleRecipientRepository.countMonthlyReceiveCapsules(phoneHash, year);

        // 3. 송신 데이터 채우기 (row[0]은 월, row[1]은 개수)
        for (Object[] row : sendData) {
            int month = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            stats.get(month - 1).setSend(count); // 리스트 인덱스는 0부터 시작하므로 month - 1
        }

        // 4. 수신 데이터 채우기
        for (Object[] row : receiveData) {
            int month = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            stats.get(month - 1).setReceive(count);
        }

        return stats;

    }

    public DailyUnlockedCapsuleResponse dailyUnlockedCapsule(Long memberId) {
        String phoneHash = memberRepository.findById(memberId).orElseThrow(() ->
                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).getPhoneHash();  // 사용자의 해시된 폰 번호

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<CapsuleRecipient> todayUnlockedCapsules = capsuleRecipientRepository.findTodayUnlockedCapsules(phoneHash, startOfDay, endOfDay);
        List<UnlockedCapsuleResponse> list = todayUnlockedCapsules.stream()
                .map(UnlockedCapsuleResponse::new)
                .toList();

        return new DailyUnlockedCapsuleResponse(list);
    }

    private Pageable createPageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50); // max 50

        return PageRequest.of(safePage, safeSize, sort);
    }

    // 스토리트랙용 캡슐 목록 조회(내가 만든 캡슐, 공개 ,장소 기반)
    public PageResponse<CapsuleDashBoardResponse> myPublicLocationCapsule (Long memberId, int page, int size){

        Pageable pageable = createPageable(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "capsuleId")
        );

        Page<Capsule> capsulePage =
                capsuleRepository.findMyCapsulesLocationType(
                        memberId,
                        "PUBLIC",
                        List.of("LOCATION", "TIME_AND_LOCATION"),
                        pageable
                );

        Page<CapsuleDashBoardResponse> responsePage = capsulePage.map(CapsuleDashBoardResponse::new);

        return new PageResponse<>(responsePage);
    }
}
