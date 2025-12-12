package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.Capsule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface CapsuleRepository extends JpaRepository<Capsule, Long> {

    // 기본: 삭제되지 않은 캡슐만 조회
    Page<Capsule> findByIsDeletedFalse(Pageable pageable);

    // visibility 필터 (PUBLIC / PRIVATE)
    Page<Capsule> findByIsDeletedFalseAndVisibility(String visibility, Pageable pageable);

    // TODO: 작성자, 기간, 키워드 검색 등은 추후 QueryDsl / Specification 으로 확장
    // 공개 캡슐이고 삭제되지 않았으며, 위치 정보가 유효한 캡슐 조회
    @Query("SELECT c FROM Capsule c " +
            "WHERE c.visibility = :visibility " +
            "AND c.isDeleted = :isDeleted " +
            "AND c.locationLat IS NOT NULL " +
            "AND c.locationLng IS NOT NULL")
    List<Capsule> findOpenCapsule(@Param("visibility") String visibility, @Param("isDeleted") int isDeleted);

    //memberId와 isDeleted=0 조건을 만족하는 Capsule 목록 조회
    @Query("SELECT c FROM Capsule c WHERE c.memberId.memberId = :memberId AND c.isDeleted = 0")
    List<Capsule> findActiveCapsulesByMemberId(@Param("memberId") Long memberId);

    Optional<Capsule> findById(Long capsuleId);
}
