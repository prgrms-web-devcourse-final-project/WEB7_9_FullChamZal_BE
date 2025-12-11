package back.fcz.domain.member.repository;

import back.fcz.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUserId(String userId);

    boolean existsByNickname(String nickname);

    boolean existsByPhoneHash(String phoneHash);

    Optional<Member> findByUserId(String userId);

    Optional<Member> findByPhoneHash(String hashedPhone);
}
