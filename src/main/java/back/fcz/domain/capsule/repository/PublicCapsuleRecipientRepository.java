package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PublicCapsuleRecipientRepository extends JpaRepository<PublicCapsuleRecipient, Long> {
    @Query("""
    select p
    from PublicCapsuleRecipient p
    join p.capsuleId c
    join c.memberId m
    where c.capsuleId = :capsuleId
      and m.phoneHash = :phoneHash
""")
    Optional<PublicCapsuleRecipient> findByCapsuleIdAndPhoneHash(
            @Param("capsuleId") Long capsuleId,
            @Param("phoneHash") String phoneHash
    );


    // 특정 Capsule ID와 Member ID가 모두 일치하는 레코드가 존재하는지 확인.
    boolean existsByCapsuleId_CapsuleIdAndMemberId(Long capsuleId, Long memberId);

    // 사용자가 열람한 공개 캡슐의 ID 목록 조회
    @Query("SELECT p.capsuleId.capsuleId FROM PublicCapsuleRecipient p WHERE p.memberId = :memberId")
    Set<Long> findViewedCapsuleIdsByMemberId(Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PublicCapsuleRecipient pcr where pcr.capsuleId.capsuleId in :capsuleIds")
    int deleteByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);
}
