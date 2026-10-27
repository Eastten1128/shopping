package com.jdr1128.shopping.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 매 요청마다 JWT를 검사하는 필터
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   CustomUserDetailsService customUserDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, //http 요청
            HttpServletResponse response, //http 응답
            FilterChain filterChain //
    ) throws ServletException, IOException {

        //request에서 토큰을 추출
        String token = resolveToken(request);

        //토큰 검증
        if (token != null && jwtTokenProvider.validateAccessToken(token)) {

            //loginId 추출
            String loginId = jwtTokenProvider.getLoginId(token);
            //추출한 loginId로 사용자 정보 조회
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginId);

            //Authentication 객체 생성
            //Spring Security에게 다음을 알려준다
            //principal = 로그인 사용자 정보
            //credentials = 비밀번호 같은 인증 수단, JWT 인증 후에는 null,
            //userDetails.getAuthorities()
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            //SecurityContextHolder에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        //다음 필터(혹은 Controller)로 넘어가기
        filterChain.doFilter(request, response);
    }

    //Authorization 헤더에서 실제 JWT만 잘라내는 함수
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
