package com.mondaycloset.shop.domain.member;

import com.mondaycloset.shop.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 인증(email/password)과 배송용 기본 정보를 함께 가진다.
 * MVP 범위에서는 배송지를 별도 테이블로 분리하지 않고 단일 주소 컬럼으로 denormalize —
 * 주소록(여러 배송지) 기능이 실제로 필요해지는 시점에 address 테이블 분리를 검토한다.
 */
@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    /** 가입 경로(LOCAL/GOOGLE). GOOGLE 계정은 password에 무작위 해시가 들어가고 실제로 사용되지 않는다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberProvider provider;

    @Builder
    private Member(String email, String password, String name, String phone, String address,
                    MemberRole role, MemberProvider provider) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.role = role == null ? MemberRole.ROLE_USER : role;
        this.status = MemberStatus.ACTIVE;
        this.provider = provider == null ? MemberProvider.LOCAL : provider;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeProfile(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ROLE_ADMIN;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
