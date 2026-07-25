package com.mondaycloset.shop.security;

import com.mondaycloset.shop.domain.member.Member;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * 구글 OAuth2(OIDC) 로그인 주체.
 * 구글이 내려준 원본 OidcUser(delegate)를 감싸면서, 우리 DB의 회원 정보(memberId, role)를 함께 들고 다닌다.
 * 컨트롤러/서비스에서는 CustomUserDetails와 동일하게 AppUserPrincipal로 다룬다.
 */
@Getter
public class CustomOAuth2User implements OidcUser, AppUserPrincipal {

    private final OidcUser delegate;
    private final Long memberId;
    private final List<GrantedAuthority> authorities;

    public CustomOAuth2User(OidcUser delegate, Member member) {
        this.delegate = delegate;
        this.memberId = member.getId();
        this.authorities = List.of(new SimpleGrantedAuthority(member.getRole().name()));
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
