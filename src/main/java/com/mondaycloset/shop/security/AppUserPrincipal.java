package com.mondaycloset.shop.security;

/**
 * 폼 로그인(CustomUserDetails)과 구글 OAuth2 로그인(CustomOAuth2User)이 공통으로 구현하는 인터페이스.
 * 컨트롤러/서비스는 로그인 방식과 무관하게 이 타입 하나로 memberId만 꺼내 쓴다.
 */
public interface AppUserPrincipal {
    Long getMemberId();
}
