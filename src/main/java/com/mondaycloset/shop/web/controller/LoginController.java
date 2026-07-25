package com.mondaycloset.shop.web.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    // SecurityConfig가 client-id 미설정 시 oauth2Login() 자체를 등록하지 않으므로,
    // 그 상태에서 구글 로그인 버튼을 노출하면 클릭 시 404가 난다 - 같은 조건으로 버튼도 숨긴다.
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    // 데모 관리자 계정은 local 프로필에서만 시드되므로(prod엔 없음), 힌트도 local에서만 노출한다.
    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                             @RequestParam(required = false) String expired,
                             HttpSession session, Model model) {
        if (error != null) {
            Object message = session.getAttribute("loginErrorMessage");
            model.addAttribute("errorMessage", message != null ? message : "로그인에 실패했습니다.");
            session.removeAttribute("loginErrorMessage");
        }
        if (expired != null) {
            model.addAttribute("errorMessage", "다른 기기에서 로그인되어 세션이 만료되었습니다.");
        }
        model.addAttribute("googleLoginEnabled", !googleClientId.isBlank());
        model.addAttribute("showDemoHint", "local".equals(activeProfile));
        return "member/login";
    }
}
