package com.mondaycloset.shop.security;

import com.mondaycloset.shop.repository.LoginHistoryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 무차별 대입(Brute Force) 방어 - 최근 N분 내 로그인 실패가 임계치 이상이면 잠긴 것으로 판단한다(S-02).
 * 별도 locked_until/failed_count 컬럼 없이 기존 login_history 이력만으로 판정한다 - 실패가
 * 뜸해지면(창 밖으로 밀려나면) 자연히 잠금이 풀리는 슬라이딩 윈도우 방식.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginAttemptService {

    public static final int LOCK_WINDOW_MINUTES = 10;
    public static final long LOCK_THRESHOLD = 5;

    private final LoginHistoryRepository loginHistoryRepository;

    public boolean isLocked(String email) {
        return recentFailureCount(email) >= LOCK_THRESHOLD;
    }

    public long recentFailureCount(String email) {
        return loginHistoryRepository.countByEmailAttemptedAndSuccessFalseAndLoginAtAfter(
                email, LocalDateTime.now().minusMinutes(LOCK_WINDOW_MINUTES));
    }
}
