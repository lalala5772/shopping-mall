package com.mondaycloset.shop.security;

import com.mondaycloset.shop.domain.member.Member;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Spring Security가 다루는 인증 주체. 컨트롤러에서 @AuthenticationPrincipal로 주입받아 memberId만 꺼내 서비스로 전달한다. */
@Getter
public class CustomUserDetails implements UserDetails, AppUserPrincipal {

    private final Long memberId;
    private final String email;
    private final String password;
    private final String name;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(Member member) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.password = member.getPassword();
        this.name = member.getName();
        this.enabled = member.isActive();
        this.authorities = List.of(new SimpleGrantedAuthority(member.getRole().name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
