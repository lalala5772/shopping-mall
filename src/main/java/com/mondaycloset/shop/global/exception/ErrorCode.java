package com.mondaycloset.shop.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 비즈니스 에러 코드.
 * 사용자에게는 message만 노출하고, 스택트레이스나 내부 구현은 절대 노출하지 않는다(GlobalExceptionHandler 참고).
 */
@Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // Member
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    WITHDRAWN_MEMBER(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    PRODUCT_NOT_PURCHASABLE(HttpStatus.CONFLICT, "판매 중인 상품이 아닙니다."),

    // Cart
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."),
    EMPTY_CART(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 주문만 조회할 수 있습니다."),
    INVALID_ORDER_STATUS_CHANGE(HttpStatus.CONFLICT, "변경할 수 없는 주문 상태입니다."),

    // Auth / Access
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // System
    CONCURRENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "다른 요청과 충돌이 발생했습니다. 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
