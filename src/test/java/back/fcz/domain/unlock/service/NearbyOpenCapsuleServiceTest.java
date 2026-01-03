package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.repository.CapsuleLikeRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.repository.PublicCapsuleRecipientRepository;
import back.fcz.domain.unlock.dto.request.NearbyOpenCapsuleRequest;
import back.fcz.domain.unlock.dto.response.NearbyOpenCapsuleResponse;
import back.fcz.domain.unlock.dto.response.projection.NearbyOpenCapsuleProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NearbyOpenCapsuleServiceTest {
    private NearbyOpenCapsuleService nearbyOpenCapsuleService;
    private PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;
    private CapsuleRepository capsuleRepository;
    private CapsuleLikeRepository capsuleLikeRepository;
    private UnlockService unlockService;

    @BeforeEach
    void setUp() {
        publicCapsuleRecipientRepository = mock(PublicCapsuleRecipientRepository.class);
        capsuleRepository = mock(CapsuleRepository.class);
        capsuleLikeRepository = mock(CapsuleLikeRepository.class);
        unlockService = mock(UnlockService.class);
        nearbyOpenCapsuleService = new NearbyOpenCapsuleService(publicCapsuleRecipientRepository, capsuleRepository, capsuleLikeRepository, unlockService);
    }

    // 테스트에 사용할 사용자 위치. 서울 시청 위도, 경도
    private double userLat = 37.5665;
    private double userLng = 126.9780;

    @Test
    @DisplayName("요청 반경 내에 캡슐만 조회되며, 사용자-캡슐 거리를 기준으로 오름차순 정렬된다")
    void getNearbyOpenCapsules_success() {
        // given
        Long memberId = 1L;
        when(publicCapsuleRecipientRepository.findViewedCapsuleIdsByMemberId(memberId))
                .thenReturn(java.util.Set.of(1L));

        NearbyOpenCapsuleProjection capsuleProjection1 = mock(NearbyOpenCapsuleProjection.class);
        NearbyOpenCapsuleProjection capsuleProjection2 = mock(NearbyOpenCapsuleProjection.class);
        NearbyOpenCapsuleProjection capsuleProjection3 = mock(NearbyOpenCapsuleProjection.class);

        when(capsuleProjection1.capsuleId()).thenReturn(1L);
        when(capsuleProjection1.locationName()).thenReturn("locationName");
        when(capsuleProjection1.nickname()).thenReturn("nick");
        when(capsuleProjection1.title()).thenReturn("title");
        when(capsuleProjection1.content()).thenReturn("content");
        when(capsuleProjection1.createdAt()).thenReturn(LocalDateTime.now());
        when(capsuleProjection1.unlockType()).thenReturn("LOCATION");
        when(capsuleProjection1.locationLat()).thenReturn(37.5674);
        when(capsuleProjection1.locationLng()).thenReturn(126.9780);
        when(capsuleProjection1.likeCount()).thenReturn(0);

        when(capsuleProjection2.capsuleId()).thenReturn(2L);
        when(capsuleProjection2.locationName()).thenReturn("locationName");
        when(capsuleProjection2.nickname()).thenReturn("nick");
        when(capsuleProjection2.title()).thenReturn("title");
        when(capsuleProjection2.content()).thenReturn("content");
        when(capsuleProjection2.createdAt()).thenReturn(LocalDateTime.now());
        when(capsuleProjection2.unlockType()).thenReturn("LOCATION");
        when(capsuleProjection2.locationLat()).thenReturn(37.5709);
        when(capsuleProjection2.locationLng()).thenReturn(126.9780);
        when(capsuleProjection2.likeCount()).thenReturn(0);

        when(capsuleProjection3.capsuleId()).thenReturn(3L);
        when(capsuleProjection3.locationName()).thenReturn("locationName");
        when(capsuleProjection3.nickname()).thenReturn("nick");
        when(capsuleProjection3.title()).thenReturn("title");
        when(capsuleProjection3.content()).thenReturn("content");
        when(capsuleProjection3.createdAt()).thenReturn(LocalDateTime.now());
        when(capsuleProjection3.unlockType()).thenReturn("LOCATION");
        when(capsuleProjection3.locationLat()).thenReturn(37.5845);
        when(capsuleProjection3.locationLng()).thenReturn(127.0000);
        when(capsuleProjection3.likeCount()).thenReturn(0);

        List<NearbyOpenCapsuleProjection> mockCapsules = Arrays.asList(capsuleProjection1, capsuleProjection2, capsuleProjection3);

        when(capsuleRepository.findNearbyCapsules(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(mockCapsules);

        // 사용자와 capsule1 사이의 거리: 100m, 사용자와 capsule2 사이의 거리: 500m, 사용자와 capsule3 사이의 거리: 2000m
        when(unlockService.calculateDistanceInMeters(eq(37.5674), eq(126.9780), eq(userLat), eq(userLng)))
                .thenReturn(100.0);
        when(unlockService.calculateDistanceInMeters(eq(37.5709), eq(126.9780), eq(userLat), eq(userLng)))
                .thenReturn(500.0);
        when(unlockService.calculateDistanceInMeters(eq(37.5845), eq(127.0000), eq(userLat), eq(userLng)))
                .thenReturn(2000.0);

        // Request DTO 생성 (반경 1000m 설정)
        NearbyOpenCapsuleRequest request = new NearbyOpenCapsuleRequest(userLat, userLng, 1000);

        // when
        List<NearbyOpenCapsuleResponse> result = nearbyOpenCapsuleService.getNearbyOpenCapsules(memberId, request);

        // then
        assertThat(result).hasSize(2);

        // 오름차순 정렬 검증 (1이 100m, 2가 500m이므로 1 -> 2 순서여야 함)
        assertThat(result.get(0).capsuleId()).isEqualTo(1L);
        assertThat(result.get(0).distanceToCapsule()).isEqualTo(100.0);
        assertThat(result.get(1).capsuleId()).isEqualTo(2L);
        assertThat(result.get(1).distanceToCapsule()).isEqualTo(500.0);
    }

    @Test
    @DisplayName("radius가 null일 경우, 기본값 1000m가 사용된다")
    void getNearbyOpenCapsules_default_radius() {
        // given
        Long memberId = 1L;
        when(publicCapsuleRecipientRepository.findViewedCapsuleIdsByMemberId(memberId))
                .thenReturn(java.util.Set.of(1L));

        NearbyOpenCapsuleProjection capsuleProjection = mock(NearbyOpenCapsuleProjection.class);

        when(capsuleProjection.capsuleId()).thenReturn(1L);
        when(capsuleProjection.locationName()).thenReturn("locationName");
        when(capsuleProjection.nickname()).thenReturn("nick");
        when(capsuleProjection.title()).thenReturn("title");
        when(capsuleProjection.content()).thenReturn("content");
        when(capsuleProjection.createdAt()).thenReturn(LocalDateTime.now());
        when(capsuleProjection.unlockType()).thenReturn("LOCATION");
        when(capsuleProjection.locationLat()).thenReturn(37.5674);
        when(capsuleProjection.locationLng()).thenReturn(126.9780);
        when(capsuleProjection.likeCount()).thenReturn(0);

        when(capsuleRepository.findNearbyCapsules(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(capsuleProjection));

        // 사용자와 capsule 사이의 거리: 500m
        when(unlockService.calculateDistanceInMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(500.0);

        // Request DTO 생성 (반경 null 설정)
        NearbyOpenCapsuleRequest request = new NearbyOpenCapsuleRequest(userLat, userLng, null);

        // when
        List<NearbyOpenCapsuleResponse> result = nearbyOpenCapsuleService.getNearbyOpenCapsules(memberId, request);

        // then
        // 기본값 1000m 내에 500m 캡슐이 있으므로 결과가 포함되어야 함
        assertThat(result).hasSize(1);
    }
}
