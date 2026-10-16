package com.jdr1128.shopping.domain.member.service;

import com.jdr1128.shopping.domain.auth.service.RefreshTokenService;
import com.jdr1128.shopping.domain.member.dto.*;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.exception.DuplicateEmailException;
import com.jdr1128.shopping.domain.member.exception.DuplicateLoginIdException;
import com.jdr1128.shopping.domain.member.exception.LoginFailedException;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.global.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService {

    @Autowired
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    //jwt
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public MemberService(MemberRepository memberRepository,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider jwtTokenProvider,
                         RefreshTokenService refreshTokenService
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    //회원 가입
    @Transactional
    public MemberResponse signup(MemberSignupRequest request){
        validateDuplicateLoginId(request.getLoginId());
        validateDuplicateEmail(request.getEmail());

        //패스워드 인코딩
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        //인코딩한 패스워드로 Member 객체 생성
        Member member = new Member(
                request.getLoginId(),
                encodedPassword,
                request.getEmail(),
                Role.USER
        );

        Member savedMember = memberRepository.save(member);
        return MemberResponse.from(savedMember);
    }

    //로그인
    public LoginResult login(LoginRequest request) {
        Member member = memberRepository.findByLoginIdAndIsDeletedFalse(request.getLoginId())
                .orElseThrow(LoginFailedException::new);

        if(!passwordEncoder.matches(request.getPassword(), member.getPassword())){
            throw new LoginFailedException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                member.getMemberId(),
                member.getLoginId(),
                member.getRole()
        );

        //RefreshToken 기반 재발급
        String refreshToken = refreshTokenService.issueRefreshToken(member);

        LoginResponse loginResponse =  LoginResponse.of(
                accessToken,
                member.getMemberId(),
                member.getLoginId(),
                member.getRole()
        );

        return new LoginResult(loginResponse, refreshToken);
    }

    //AccessToken 재발급
    @Transactional
    public LoginResult refresh(String refreshToken){
        return refreshTokenService.rotateRefreshToken(refreshToken);
    }

    //로그아웃
    @Transactional
    public void logout(String refreshToken){
        refreshTokenService.logout(refreshToken);
    }


    //중복 검사 - LoginId
    private void validateDuplicateLoginId(String loginId){
        if(memberRepository.existsByLoginId(loginId)) {
            throw new DuplicateLoginIdException();
        }
    }

    //중복 검사 - Email
    private void validateDuplicateEmail(String email){
        if(memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
    }
}
