package com.mondaycloset.shop.security;

import com.mondaycloset.shop.domain.security.LoginHistory;
import com.mondaycloset.shop.repository.LoginHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 실패 시 login_history에 실패 기록을 남기고, 최근 반복 실패 횟수를 안내한다.
 * 실제 잠금 판정/차단은 LoginAttemptService + CustomUserDetailsService가 수행하고,
 * 여기서는 그 결과(LockedException)에 맞는 안내 메시지만 보여준다.
 * (비정상 로그인 시도 추적 - 발표자료 6p)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginHistoryRepository loginHistoryRepository;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws java.io.IOException {
        String email = request.getParameter("email");
        if (email == null) {
            email = "unknown";
        }

        loginHistoryRepository.save(LoginHistory.builder()
                .member(null)
                .emailAttempted(email)
                .ipAddress(LoginSuccessHandler.extractIp(request))
                .success(false)
                .build());

        long recentFailures = loginAttemptService.recentFailureCount(email);

        log.warn("[Login Failure] email={}, reason={}, recentFailures={}",
                email, exception.getClass().getSimpleName(), recentFailures);

        String message;
        // CustomUserDetailsService(loadUserByUsername)에서 던진 LockedException은 DaoAuthenticationProvider가
        // InternalAuthenticationServiceException으로 한 번 감싸서 전달한다 - UsernameNotFoundException과 달리
        // LockedException은 특별 취급 대상이 아니기 때문. 그래서 원인(cause)까지 함께 확인해야 한다.
        if (exception instanceof LockedException || exception.getCause() instanceof LockedException) {
            message = String.format(
                    "로그인 시도가 너무 많아 계정이 잠겼습니다. %d분 후 다시 시도해 주세요.",
                    LoginAttemptService.LOCK_WINDOW_MINUTES);
        } else if (exception instanceof DisabledException) {
            message = "탈퇴한 계정입니다.";
        } else {
            message = "이메일 또는 비밀번호가 올바르지 않습니다.";
            if (recentFailures >= LoginAttemptService.LOCK_THRESHOLD) {
                message += String.format(" (최근 %d분 내 %d회 실패 - 계정 보호를 위해 잠시 후 다시 시도해 주세요.)",
                        LoginAttemptService.LOCK_WINDOW_MINUTES, recentFailures);
            }
        }

        request.getSession().setAttribute("loginErrorMessage", message);
        response.sendRedirect("/login?error");
    }
}
