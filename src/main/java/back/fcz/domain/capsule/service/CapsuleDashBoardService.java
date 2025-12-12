package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CapsuleDashBoardService {
    private final CapsuleRepository capsuleRepository;

    public List<Capsule> readSendCapsuleList(Long memberId) {
        return capsuleRepository.findActiveCapsulesByMemberId(memberId);
    }

/*
    public List<Capsule> readReceiveCapsuleList(Long memberId) {

    }
    */
}
