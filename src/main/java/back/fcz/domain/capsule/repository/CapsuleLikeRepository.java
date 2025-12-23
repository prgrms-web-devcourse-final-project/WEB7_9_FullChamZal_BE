package back.fcz.domain.capsule.repository;


import back.fcz.domain.capsule.entity.CapsuleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface CapsuleLikeRepository extends JpaRepository<CapsuleLike, Long> {
    @Query("SELECT COUNT(c) > 0 " +
            "FROM CapsuleLike c " +
            "WHERE c.capsuleId.capsuleId = :capsuleId " +
            "AND c.memberId.memberId = :memberId")
    boolean existsByCapsuleIdMemberId(@Param("capsuleId") Long capsuleId, @Param("memberId") Long memberId);

    void deleteByCapsuleId_CapsuleIdAndMemberId_MemberId(Long capsuleId, Long memberId);

    // 특정 사용자가 좋아요를 누른 캡슐의 ID 리스트 조회
    @Query("SELECT c.capsuleId.capsuleId FROM CapsuleLike c WHERE c.memberId.memberId = :memberId")
    Set<Long> findAllLikedCapsuleIdsByMemberId(@Param("memberId") Long memberId);
}
