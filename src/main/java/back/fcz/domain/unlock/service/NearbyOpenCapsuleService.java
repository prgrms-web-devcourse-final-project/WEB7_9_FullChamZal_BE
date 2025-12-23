package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleLikeRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.unlock.dto.request.NearbyOpenCapsuleRequest;
import back.fcz.domain.unlock.dto.response.NearbyOpenCapsuleResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NearbyOpenCapsuleService {
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleLikeRepository capsuleLikeRepository;
    private final UnlockService unlockService;

    private final int DEFAULT_RADIUS_M = 1000;  // 기본 반경 값 1km

    // 사용자 근처 공개 캡슐 리스트 조회
    @Transactional
    public List<NearbyOpenCapsuleResponse> getNearbyOpenCapsules(long memberId, NearbyOpenCapsuleRequest request) {

        LocalDateTime currentTime = LocalDateTime.now();
        double currentLat = request.currentLatitude();
        double currentLng = request.currentLongitude();
        int searchRadiusM = (request.radius() == null) ? DEFAULT_RADIUS_M : request.radius();

        if(searchRadiusM != 500 && searchRadiusM != 1000 && searchRadiusM != 1500) {
            throw new BusinessException(ErrorCode.INVALID_RADIUS);
        }

        // 공개 캡슐은 위치 정보가 기본이므로, 해제 조건이 시간인지, 위치인지 필터링 불필요
        // 공개 + 삭제되지 않은 캡슐 전체 조회
        List<Capsule> capsules = capsuleRepository.findOpenCapsule("PUBLIC", 0);

        // 사용자가 열람한 공개 캡슐의 ID 목록 조회
        Set<Long> viewedCapsuleIds = publicCapsuleRecipientRepository.findViewedCapsuleIdsByMemberId(memberId);

        // 사용자가 좋아요를 누른 캡슐 ID 목록 조회
        Set<Long> likedCapsuleIds = capsuleLikeRepository.findAllLikedCapsuleIdsByMemberId(memberId);

        return capsules.stream()
                .map(capsule -> {
                    // 캡슐 위치와 사용자 위치 간 거리 계산
                    double distance = unlockService.calculateDistanceInMeters(
                            capsule.getLocationLat(), capsule.getLocationLng(), currentLat, currentLng
                    );

                    // 요청 반경(searchRadiusM)을 벗어나면 null 반환
                    if (distance > searchRadiusM) {
                        return null;
                    }

                    // 사용자가 해당 캡슐을 열람한 적 있는 지, 확인 (열람했다면 true, 미열람이라면 false)
                    boolean isViewed = viewedCapsuleIds.contains(capsule.getCapsuleId());

                    // 사용자가 해당 캡슐을 열람할 수 있는 지, 확인 (열람할 수 있다면 true, 열람 불가하다면 false)
                    boolean isUnlockable = unlockService.validateTimeAndLocationConditions(
                            capsule, currentTime, currentLat, currentLng
                    );

                    // 사용자가 해당 캡슐에 좋아요를 눌렀는 지, 확인 (좋아요 했다면 true, 좋아요 안했다면 false)
                    boolean isLiked = likedCapsuleIds.contains(capsule.getCapsuleId());

                    // 응답 DTO로 매핑
                    return new NearbyOpenCapsuleResponse(capsule, distance, isViewed, isUnlockable, isLiked);
                })
                // distance > searchRadiusM인 항목 제거
                .filter(response -> response != null)

                // 사용자와 캡슐간 거리 차이를 기준으로 오름차순 정렬
                .sorted(Comparator.comparingDouble(NearbyOpenCapsuleResponse::distanceToCapsule))

                .collect(Collectors.toList());
    }
}
