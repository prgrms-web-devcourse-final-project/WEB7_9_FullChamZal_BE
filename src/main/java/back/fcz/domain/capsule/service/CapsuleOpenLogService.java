package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapsuleOpenLogService {

    private final CapsuleOpenLogRepository capsuleOpenLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLogInNewTransaction(CapsuleOpenLog openLog) {
        capsuleOpenLogRepository.save(openLog);
        log.debug("독립 트랜잭션으로 로그 저장 완료 - capsuleId: {}, status: {}",
                openLog.getCapsuleId().getCapsuleId(), openLog.getStatus());
    }
}
