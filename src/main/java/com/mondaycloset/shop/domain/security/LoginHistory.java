package com.mondaycloset.shop.domain.security;

import com.mondaycloset.shop.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 성공/실패 이력. 비정상 로그인 시도 탐지의 근거 데이터가 된다(발표자료 6p 보안 설계).
 * 실패 시에는 member_id를 특정할 수 없는 경우(존재하지 않는 이메일)가 있으므로 member는 nullable,
 * emailAttempted에 원본 시도 이메일을 별도로 남긴다.
 */
@Getter
@Entity
@Table(name = "login_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "email_attempted", nullable = false, length = 100)
    private String emailAttempted;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private boolean success;

    @Builder
    private LoginHistory(Member member, String emailAttempted, String ipAddress, boolean success) {
        this.member = member;
        this.emailAttempted = emailAttempted;
        this.ipAddress = ipAddress;
        this.success = success;
        this.loginAt = LocalDateTime.now();
    }
}
