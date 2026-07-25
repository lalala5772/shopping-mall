package com.mondaycloset.shop.security;

import com.mondaycloset.shop.domain.member.Member;
import com.mondaycloset.shop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final LoginAttemptService loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 비밀번호를 대조하기 전에 먼저 잠금 여부를 확인한다 - 그래야 맞는 비밀번호로도
        // 잠금 기간 중에는 로그인이 되지 않는다(S-02, 지금까지는 경고만 하고 실제로 막지 않던 버그).
        if (loginAttemptService.isLocked(email)) {
            throw new LockedException("로그인 시도 횟수 초과로 계정이 잠겼습니다: " + email);
        }
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("가입되지 않은 이메일입니다: " + email));
        return new CustomUserDetails(member);
    }
}
