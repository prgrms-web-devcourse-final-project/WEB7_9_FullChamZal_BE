package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.Capsule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CapsuleRepository extends JpaRepository<Capsule, Long> {
  
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
}
