package com.mondaycloset.shop.global.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 전역 예외 처리.
 * 원칙: 사용자에게는 ErrorCode.message만 노출한다. 스택트레이스/쿼리/내부 클래스명은 절대 노출하지 않는다.
 * (요구사항추적표 S-04 대응)
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e, HttpServletRequest request,
                                                 RedirectAttributes redirectAttributes) {
        log.warn("[BusinessException] {} - {}", e.getErrorCode(), e.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && isSameOrigin(request, referer)) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // ModelAndView의 viewName에 "redirect:" 접두사를 그대로 사용하면 일반 컨트롤러의
            // "redirect:..." 문자열 반환과 동일하게 RedirectView로 처리된다(플래시 속성도 정상 동작).
            return new ModelAndView("redirect:" + referer);
        }
        ModelAndView mav = new ModelAndView("error/business-error");
        mav.addObject("message", e.getMessage());
        mav.setStatus(e.getErrorCode().getHttpStatus());
        return mav;
    }

    /**
     * Referer 헤더는 클라이언트가 보내는 값이라 조작이 가능하다. 검증 없이 그대로
     * "redirect:" + referer 로 리다이렉트하면 외부 사이트로 보내는 오픈 리다이렉트가 된다
     * (예: evil.com에서 이 앱으로 폼을 제출하도록 유도 -> 비즈니스 예외 발생 -> evil.com으로 리다이렉트).
     * 같은 오리진(host)일 때만 신뢰한다.
     */
    private boolean isSameOrigin(HttpServletRequest request, String referer) {
        try {
            URI uri = URI.create(referer);
            String host = uri.getHost();
            return host != null && host.equalsIgnoreCase(request.getServerName());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ModelAndView handleOptimisticLock(Exception e, HttpServletRequest request) {
        log.warn("[OptimisticLock] concurrent update conflict at {}", request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/business-error");
        mav.addObject("message", ErrorCode.CONCURRENT_UPDATE_CONFLICT.getMessage());
        mav.setStatus(ErrorCode.CONCURRENT_UPDATE_CONFLICT.getHttpStatus());
        return mav;
    }

    /**
     * @Version 충돌이 서비스 메서드 반환 이후 커밋 시점 flush에서 발생하면
     * Spring이 TransactionSystemException으로 감싸는 경우가 있다. 근본 원인이 낙관적 락이면 동일하게 처리한다.
     */
    @ExceptionHandler(TransactionSystemException.class)
    public ModelAndView handleTransactionSystemException(TransactionSystemException e, HttpServletRequest request) {
        Throwable root = e.getRootCause();
        if (root instanceof OptimisticLockException || root instanceof OptimisticLockingFailureException) {
            return handleOptimisticLock(e, request);
        }
        return handleUnexpected(e, request);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception e, HttpServletRequest request) {
        // 내부 원인은 서버 로그에만 남기고, 사용자 화면에는 절대 노출하지 않는다.
        log.error("[UnhandledException] {} at {}", e.getClass().getSimpleName(), request.getRequestURI(), e);
        ModelAndView mav = new ModelAndView("error/business-error");
        mav.addObject("message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        mav.setStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}
