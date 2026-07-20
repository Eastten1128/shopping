package com.jdr1128.shopping.domain.member.controller;

import com.jdr1128.shopping.domain.member.dto.LoginRequest;
import com.jdr1128.shopping.domain.member.dto.LoginResponse;
import com.jdr1128.shopping.domain.member.dto.MemberResponse;
import com.jdr1128.shopping.domain.member.dto.MemberSignupRequest;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.service.MemberService;
import com.jdr1128.shopping.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
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
        LoginResponse response = memberService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        MemberResponse response = MemberResponse.from(userDetails.getMember());

        return ResponseEntity.ok(response);
    }
}
