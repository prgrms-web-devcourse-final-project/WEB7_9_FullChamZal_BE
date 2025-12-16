package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 선착순 캡슐 동시성 제어
@Slf4j
@Service
@RequiredArgsConstructor
public class FirstComeService {

    private final CapsuleRepository capsuleRepository;


    public boolean hasFirstComeLimit(Capsule capsule) {

        return false;
    }
}
