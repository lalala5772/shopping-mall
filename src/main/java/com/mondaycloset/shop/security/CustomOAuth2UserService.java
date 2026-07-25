package com.mondaycloset.shop.security;

import com.mondaycloset.shop.domain.member.Member;
import com.mondaycloset.shop.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * 구글 로그인 성공 시 호출된다. 구글이 확인해 준 이메일을 기준으로 회원을 찾거나(계정 연동),
 * 없으면 자동 가입시킨다(MemberService.findOrCreateOAuthMember).
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final MemberService memberService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("구글 계정에서 이메일 정보를 확인할 수 없습니다.");
        }
        String name = oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank()
                ? oidcUser.getFullName()
                : email;

        Member member = memberService.findOrCreateOAuthMember(email, name);

        return new CustomOAuth2User(oidcUser, member);
    }
}
