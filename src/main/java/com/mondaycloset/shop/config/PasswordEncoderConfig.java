package com.mondaycloset.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder를 SecurityConfig에서 분리한 이유: SecurityConfig가 CustomOAuth2UserService를
 * 생성자로 주입받는데, CustomOAuth2UserService -> MemberService -> PasswordEncoder로 이어지는
 * 의존이 다시 SecurityConfig(빈 정의 위치)로 돌아오면서 순환 참조가 발생했다.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
