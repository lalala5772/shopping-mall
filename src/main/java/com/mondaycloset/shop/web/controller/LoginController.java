package com.mondaycloset.shop.web.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

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
        return "member/login";
    }
}
