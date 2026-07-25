package com.mondaycloset.shop.security;

import com.mondaycloset.shop.domain.security.LoginHistory;
import com.mondaycloset.shop.repository.LoginHistoryRepository;
import com.mondaycloset.shop.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 성공 시 login_history에 성공 기록을 남긴다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final MemberRepository memberRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    @PostConstruct
    void configure() {
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false); // 로그인 전 접근하려던 페이지가 있으면 그쪽으로 이동
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        // 폼 로그인(CustomUserDetails)/구글 로그인(CustomOAuth2User) 모두 AppUserPrincipal이라
        // 로그인 방식과 무관하게 memberId 하나로 처리한다.
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        memberRepository.findById(principal.getMemberId()).ifPresent(member -> {
            loginHistoryRepository.save(LoginHistory.builder()
                    .member(member)
                    .emailAttempted(member.getEmail())
                    .ipAddress(extractIp(request))
                    .success(true)
                    .build());
            log.info("[Login Success] email={}", member.getEmail());
        });
        super.onAuthenticationSuccess(request, response, authentication);
    }

    static String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
