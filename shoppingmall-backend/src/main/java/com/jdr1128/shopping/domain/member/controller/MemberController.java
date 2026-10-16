package com.jdr1128.shopping.domain.member.controller;

import com.jdr1128.shopping.domain.member.dto.LoginRequest;
import com.jdr1128.shopping.domain.member.dto.LoginResponse;
import com.jdr1128.shopping.domain.member.dto.LoginResult;
import com.jdr1128.shopping.domain.member.dto.MemberResponse;
import com.jdr1128.shopping.domain.member.dto.MemberSignupRequest;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.service.MemberService;
import com.jdr1128.shopping.global.security.CustomUserDetails;
import com.jdr1128.shopping.global.security.RefreshTokenCookieProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    public MemberController(MemberService memberService, RefreshTokenCookieProvider refreshTokenCookieProvider) {
        this.memberService = memberService;
        this.refreshTokenCookieProvider = refreshTokenCookieProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(
            @Valid @RequestBody MemberSignupRequest request
    ){
        MemberResponse response = memberService.signup(request);

        return ResponseEntity
                .created(URI.create("/api/members/" + response.getMemberId()))
                .body(response);
    }

    //로그인 - 토큰을 생성해야 하므로 POST
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ){
        LoginResult loginResult = memberService.login(request);

        ResponseCookie refreshTokenCookie =
                refreshTokenCookieProvider.createRefreshTokenCookie(
                        loginResult.getRefreshToken()
                );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(loginResult.getLoginResponse());

    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        //Refresh 시행
        LoginResult loginResult = memberService.refresh(refreshToken);

        ResponseCookie refreshTokenCookie =
                refreshTokenCookieProvider.createRefreshTokenCookie(
                        loginResult.getRefreshToken()
                );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(loginResult.getLoginResponse());
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        MemberResponse response = MemberResponse.from(userDetails.getMember());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        memberService.logout(refreshToken);

        ResponseCookie expiredRefreshTokenCookie =
                refreshTokenCookieProvider.createExpiredRefreshTokenCookie(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie.toString())
                .build();
    }


}
