package back.fcz.domain.member.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.security.oauth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String sub = (String) attributes.get("sub");  // 구글에서 해당 유저에게 부여한 고유 ID 값

        Member member = memberRepository.findByUserId(email)
                .orElseGet(() -> memberRepository.save(Member.createOAuth(email, name, sub)));

        return new PrincipalDetails(member, attributes);
    }
}