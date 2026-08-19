package com.jdr1128.shopping.domain.member.service;

import com.jdr1128.shopping.domain.member.dto.LoginRequest;
import com.jdr1128.shopping.domain.member.dto.LoginResponse;
import com.jdr1128.shopping.domain.member.dto.MemberResponse;
import com.jdr1128.shopping.domain.member.dto.MemberSignupRequest;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.exception.DuplicateLoginIdException;
import com.jdr1128.shopping.domain.member.exception.LoginFailedException;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 테스트 목록
 * 회원가입 성공
 * loginId 중복 시 실패
 * email 중복 시 실패
 * 로그인 성공
 * 비밀번호 불일치 시 실패
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입에 성공한다")
    void signupSuccess(){
        //given
        MemberSignupRequest request = new MemberSignupRequest();

        ReflectionTestUtils.setField(request, "loginId", "user99");
        ReflectionTestUtils.setField(request, "password", "password1234");
        ReflectionTestUtils.setField(request, "email", "user99@test.com");

        when(memberRepository.existsByLoginId("user99")).thenReturn(false);
        when(memberRepository.existsByEmail("user99@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");

        Member savedMember = new Member(
                "user99",
                "encoded-password",
                "user99@test.com",
                Role.USER
        );
        ReflectionTestUtils.setField(savedMember, "memberId", 1L);

        when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

        //when
        MemberResponse response = memberService.signup(request);

        //then
        //이새끼 왜 실제값이 user99로 나옴?
        assertThat(response.getMemberId()).isEqualTo(1L);
        //얘는 왜 encoded-password로 나옴?
        assertThat(response.getLoginId()).isEqualTo("user99");
        assertThat(response.getEmail()).isEqualTo("user99@test.com");
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("중복 loginId가 있으면 회원가입에 실패한다.")
    void signupFailUplicateLoginId(){
        //given
        MemberSignupRequest request = new MemberSignupRequest();

        ReflectionTestUtils.setField(request, "loginId", "user01");
        ReflectionTestUtils.setField(request, "password", "password1234");
        ReflectionTestUtils.setField(request, "email", "user01@test.com");

        //중복 loginId가 있으면 실패
        when(memberRepository.existsByLoginId("user01")).thenReturn(true);

        //when & then
        assertThatThrownBy(()-> memberService.signup(request))
                .isInstanceOf(DuplicateLoginIdException.class);

    }

    @Test
    @DisplayName("로그인 성공시 JWT 반환")
    void loginSuccess(){
        //given
        LoginRequest loginRequest = new LoginRequest();

        ReflectionTestUtils.setField(loginRequest, "loginId", "user01");
        ReflectionTestUtils.setField(loginRequest, "password", "password1234");

        Member member = new Member(
                "user01",
                "encoded-password",
                "user01@test.com",
                Role.USER
        );
        ReflectionTestUtils.setField(member, "memberId", 1L);

        when(memberRepository.findByLoginIdAndIsDeletedFalse("user01"))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password1234", "encoded-password"))
                .thenReturn(true);
        when(jwtTokenProvider.createAccessToken("user01", Role.USER))
                .thenReturn("accessToken");

        //when
        LoginResponse response = memberService.login(loginRequest);

        //then

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getLoginId()).isEqualTo("user01");
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인에 실패한다")
    void loginFailWrongPassword() {
        //given
        LoginRequest request = new LoginRequest();

        ReflectionTestUtils.setField(request, "loginId", "user01");
        ReflectionTestUtils.setField(request, "password", "wrong-password");

        Member member = new Member(
                "user01",
                "encoded-password",
                "user01@test.com",
                Role.USER
        );

        when(memberRepository.findByLoginIdAndIsDeletedFalse("user01"))
                .thenReturn(Optional.of(member));

        //둘이
        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        //when & then
        assertThatThrownBy(()-> memberService.login(request))
                .isInstanceOf(LoginFailedException.class);
    }
}