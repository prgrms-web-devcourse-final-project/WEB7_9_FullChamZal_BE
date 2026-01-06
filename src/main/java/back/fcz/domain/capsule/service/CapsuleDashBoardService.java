package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.*;
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
    private final CapsuleCacheService capsuleCacheService;

    // 사용자가 전송한 캡슐 목록 조회
    public Page<CapsuleDashBoardResponse> readSendCapsuleList(Long memberId, Pageable pageable) {
        Page<Capsule> capsules = capsuleRepository.findActiveCapsulesByMemberId(memberId, pageable);
        return capsules.map(capsule -> new CapsuleDashBoardResponse(capsule));
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


    public YearlyCapsuleResponse getYearlyCapsule(Long memberId, int year) {

        String phoneHash = memberRepository.findById(memberId).orElseThrow(() ->
                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).getPhoneHash();  // 사용자의 해시된 폰 번호

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        // 1월~12월까지 0으로 초기화된 리스트 생성
        List<MonthlyCapsuleStat> stats = new ArrayList<>();

        if(year < currentYear){
            //지나간 해의 데이터는 (1월~12월까지 계산, 캐싱된 데이터)
            stats.addAll(capsuleCacheService.getCachedPastStats(memberId, year, 12, phoneHash).data());
        }else{
            //이전 달 까진 캐싱된 데이터를 사용
            stats.addAll(capsuleCacheService.getCachedPastStats(memberId, year, currentMonth - 1, phoneHash).data());

            //이번 달은 데이터 정확성을 위해 직접 계산
            stats.add(getRealTimeStat(memberId, year, currentMonth, phoneHash));

            //미래의 달(다음달~12월)은 0으로 채우기
            for (int i = currentMonth + 1; i <= 12; i++) {
                stats.add(new MonthlyCapsuleStat(i + "월", 0, 0));
            }
        }

        return new YearlyCapsuleResponse(stats);
    }

    private MonthlyCapsuleStat getRealTimeStat(Long memberId, int year, int month, String phoneHash) {
        // 이번 달의 송신/수신 개수를 각각 조회
        long sendCount = capsuleRepository.countSpecificMonthSend(memberId, year, month);
        long receiveCount = capsuleRecipientRepository.countSpecificMonthReceive(phoneHash, year, month);

        // 결과 객체 생성 및 반환
        return new MonthlyCapsuleStat(month + "월", receiveCount, sendCount);
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
                Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "capsuleId")
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
