package com.mondaycloset.shop.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 접근 거부(403) 안내 페이지.
 * SecurityConfig의 accessDeniedPage("/error/403")는 원래 요청을 서블릿 forward로 이 경로에 전달하는데,
 * forward는 원래 HTTP 메서드(GET/POST 등)를 그대로 유지한다. 그래서 GET만 받는 view controller로
 * 등록하면 POST 요청이 거부됐을 때(예: CSRF 실패) 이 페이지 자체가 405를 던지는 문제가 생긴다.
 * 어떤 메서드로 forward되어도 받을 수 있도록 일반 매핑으로 둔다.
 */
@Controller
public class ErrorPageController {

    @RequestMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
