package com.mondaycloset.shop.config;

import com.mondaycloset.shop.security.CustomOAuth2UserService;
import com.mondaycloset.shop.security.LoginFailureHandler;
import com.mondaycloset.shop.security.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증/인가 설정.
 * - 비밀번호: BCrypt 해시 (S-01)
 * - 세션 고정 공격 방어: changeSessionId 전략으로 로그인 성공 시 세션ID 재발급 (S-02)
 * - CSRF: Spring Security 기본 활성화 유지, Thymeleaf 폼에서 자동으로 토큰 포함 (S-03)
 * - 인가: /admin/** 는 ROLE_ADMIN만 접근 가능, 그 외 보호 자원은 인증만 요구 (S-05)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                // 도로명주소 API(juso.go.kr) 팝업이 주소 선택 후 이 페이지로 POST 콜백한다.
                // 외부 사이트가 우리 CSRF 토큰을 알 수 없으므로 이 경로만 CSRF 검증에서 제외한다.
                .ignoringRequestMatchers("/address-callback.html")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/address-callback.html").permitAll()
                .requestMatchers("/", "/login", "/members/register").permitAll()
                .requestMatchers("/products", "/products/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOAuth2UserService))
                .successHandler(loginSuccessHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().changeSessionId()
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            );

        return http.build();
    }
}
