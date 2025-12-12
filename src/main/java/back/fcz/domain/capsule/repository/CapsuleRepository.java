package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.Capsule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CapsuleRepository extends JpaRepository<Capsule, Long> {

    //memberId와 isDeleted=0 조건을 만족하는 Capsule 목록 조회
    @Query("SELECT c FROM Capsule c WHERE c.memberId.memberId = :memberId AND c.isDeleted = 0")
    List<Capsule> findActiveCapsulesByMemberId(@Param("memberId") Long memberId);

    //List<Capsule> findByMemberId_MemberId(Long memberId);
}
