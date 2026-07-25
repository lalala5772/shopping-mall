package com.mondaycloset.shop.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 관련 요청/응답 DTO. 엔티티를 화면/폼에 직접 노출하지 않기 위한 경계 객체.
 * Thymeleaf th:object 바인딩 호환을 위해 getter/setter를 갖는 일반 클래스로 작성한다(불변 record 대신).
 */
public class MemberDtos {

    private MemberDtos() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8자 이상이어야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "비밀번호는 영문과 숫자를 포함해야 합니다.")
        private String password;

        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        private String passwordConfirm;

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력해 주세요.")
        private String name;

        @Pattern(regexp = "^$|^01[016789]-?\\d{3,4}-?\\d{4}$",
                message = "올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)")
        private String phone;

        private String address;

        @AssertTrue(message = "이용약관 및 개인정보 수집에 동의해야 가입할 수 있습니다.")
        private boolean agreeTerms;

        /** 비밀번호/비밀번호 확인 교차 검증. Bean Validation이 isXxx() boolean 메서드를 자동으로 평가한다. */
        @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
        public boolean isPasswordConfirmed() {
            return password != null && password.equals(passwordConfirm);
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MemberInfoResponse {
        private final Long id;
        private final String email;
        private final String name;
        private final String phone;
        private final String address;
    }
}
