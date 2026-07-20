package com.jdr1128.shopping.global.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        /**
                        .requestMatchers(HttpMethod.POST, "/api/members/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/members/login").permitAll()

                        // 5, 6단계에서 만든 기능이 갑자기 막히지 않도록 8단계에서는 임시 허용
                        // 9단계 권한 처리에서 ADMIN, SELLER 권한으로 다시 제한할 예정
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                         */

                        //회원
                        .requestMatchers(HttpMethod.POST,"/api/members/signup").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/members/login").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/members/me").authenticated()

                        //카테고리 조회 - 모두 허용
                        .requestMatchers(HttpMethod.GET,"/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/categories/**").permitAll()

                        //카테고리 등록/수정/삭제 : ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        //상품 조회: 모두 허용
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        //상품 등록/수정/삭제: SELLER, ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")

                        //그 외 요청은 로그인 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
