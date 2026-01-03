package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.MonthlyCapsuleStat;
import back.fcz.domain.capsule.DTO.response.YearlyCapsuleResponse;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CapsuleCacheService {
    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;

    @Cacheable(value = "monthlyStatsPast", key = "#memberId + '_' + #year + '_' + #lastMonth")
    //이번달이 아닌 지난 달의 캡슐 송수신 횟수 계산(캐싱 사용)
    public YearlyCapsuleResponse getCachedPastStats(Long memberId, int year, int lastMonth, String phoneHash) {
        if (lastMonth <= 0){
            //이번달이 1월이면 지난달은 없으니 빈 리스트를 반환
            return new YearlyCapsuleResponse(new ArrayList<>());
        }

        List<MonthlyCapsuleStat> pastStats = new ArrayList<>();
        for (int i = 1; i <= lastMonth; i++) {
            pastStats.add(new MonthlyCapsuleStat(i + "월", 0, 0));
        }

        // 기존 DB 조회 및 데이터 매핑 로직 (1월 ~ lastMonth)
        List<Object[]> sendData = capsuleRepository.countMonthlySendCapsules(memberId, year);
        List<Object[]> receiveData = capsuleRecipientRepository.countMonthlyReceiveCapsules(phoneHash, year);

        for (Object[] row : sendData) {
            int month = ((Number) row[0]).intValue();
            if (month <= lastMonth) {
                pastStats.get(month - 1).setSend(((Number) row[1]).longValue());
            }
        }

        for (Object[] row : receiveData) {
            int month = ((Number) row[0]).intValue();
            if (month <= lastMonth) {
                pastStats.get(month - 1).setReceive(((Number) row[1]).longValue());
            }
        }

        return new YearlyCapsuleResponse(pastStats);
    }
}
