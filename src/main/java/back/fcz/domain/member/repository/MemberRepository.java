package back.fcz.domain.member.repository;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUserId(String userId);

    boolean existsByNickname(String nickname);

    boolean existsByPhoneHash(String phoneHash);

    Optional<Member> findByUserId(String userId);

    Optional<Member> findByPhoneHash(String hashedPhone);

    // userId 관련
    boolean existsByUserIdAndDeletedAtIsNull(String userId);
    Optional<Member> findByUserIdAndDeletedAtIsNotNull(String userId);

    // phoneHash 관련 (추가)
    boolean existsByPhoneHashAndDeletedAtIsNull(String phoneHash);
    Optional<Member> findByPhoneHashAndDeletedAtIsNotNull(String phoneHash);

    List<Member> findAllByStatusAndDeletedAtBefore(MemberStatus status, LocalDateTime deletedAt);
}
