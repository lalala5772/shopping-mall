package com.mondaycloset.shop.repository;

import com.mondaycloset.shop.domain.security.LoginHistory;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    // 최근 N분 내 로그인 실패 횟수 - 무차별 대입 공격(brute force) 탐지 근거로 사용
    long countByEmailAttemptedAndSuccessFalseAndLoginAtAfter(String emailAttempted, LocalDateTime after);
}
