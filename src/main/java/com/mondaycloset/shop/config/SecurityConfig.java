package com.mondaycloset.shop.config;

import com.mondaycloset.shop.security.CustomOAuth2UserService;
import com.mondaycloset.shop.security.LoginFailureHandler;
import com.mondaycloset.shop.security.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    // 구글 OAuth2 클라이언트 설정이 없는 환경(client-id 미설정)에서는 oauth2Login()을 등록하지 않는다.
    // 등록해버리면 Spring Security가 ClientRegistrationRepository 빈을 요구하게 되어
    // 그 빈이 없을 때 애플리케이션 컨텍스트 자체가 뜨지 못한다(폼 로그인까지 함께 막힘).
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

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
                // 관리자는 고객용 마이페이지/장바구니 대상이 아니므로 URL을 직접 입력해도 접근을 막는다
                // (네비게이션 메뉴만 숨기는 건 우회 가능하므로 인가 규칙으로도 동일하게 차단).
                .requestMatchers("/mypage/**", "/cart/**").hasRole("USER")
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
            );

        if (!googleClientId.isBlank()) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOAuth2UserService))
                .successHandler(loginSuccessHandler)
            );
        }

        http
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
