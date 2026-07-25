package com.mondaycloset.shop.global.exception;

import lombok.Getter;

/** 예상된 비즈니스 예외의 최상위 타입. RuntimeException이므로 서비스 계층의 트랜잭션은 자동 롤백된다. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
