package com.mondaycloset.shop.service;

import com.mondaycloset.shop.domain.cart.Cart;
import com.mondaycloset.shop.domain.member.Member;
import com.mondaycloset.shop.domain.member.MemberProvider;
import com.mondaycloset.shop.domain.member.MemberRole;
import com.mondaycloset.shop.global.exception.BusinessException;
import com.mondaycloset.shop.global.exception.ErrorCode;
import com.mondaycloset.shop.repository.CartRepository;
import com.mondaycloset.shop.repository.MemberRepository;
import com.mondaycloset.shop.web.dto.MemberDtos.MemberInfoResponse;
import com.mondaycloset.shop.web.dto.MemberDtos.RegisterRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(RegisterRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(MemberRole.ROLE_USER)
                .provider(MemberProvider.LOCAL)
                .build();
        memberRepository.save(member);

        // 회원가입과 동시에 1:1 카트를 만들어 둔다 - 이후 로직에서 "카트 없음" 분기를 없앤다.
        cartRepository.save(new Cart(member));

        return member.getId();
    }

    /**
     * 구글 OAuth2 로그인 - 이미 가입된 이메일이면 그 계정으로 로그인시키고(계정 연동),
     * 없으면 자동 가입한다. 비밀번호는 로컬 로그인에 쓰이지 않도록 무작위 값으로 채운다.
     */
    @Transactional
    public Member findOrCreateOAuthMember(String email, String name) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    Member member = Member.builder()
                            .email(email)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .name(name)
                            .role(MemberRole.ROLE_USER)
                            .provider(MemberProvider.GOOGLE)
                            .build();
                    memberRepository.save(member);
                    cartRepository.save(new Cart(member));
                    return member;
                });
    }

    public MemberInfoResponse getMemberInfo(Long memberId) {
        Member member = findMember(memberId);
        return MemberInfoResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .phone(member.getPhone())
                .address(member.getAddress())
                .build();
    }

    @Transactional
    public void updateProfile(Long memberId, String name, String phone, String address) {
        Member member = findMember(memberId);
        member.changeProfile(name, phone, address);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
