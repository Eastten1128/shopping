package com.jdr1128.shopping.global.security;

import com.jdr1128.shopping.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Member를 Spring Securty가 이해할 수 있는 사용자 객체로 감싸는 클래스
 * Spring Security는 기본적으로 UserDetails라는 인터페이스를 통해
 * 사용자 정보를 다룬다
 * 공식 문서에서도 UserDetailsService가 UserDetails를 반환하고,
 * 이 객체가 인증 결과의 principal로 사용된다고 설명한다.
 */
public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member){
        this.member = member;
    }

    public Member getMember(){
        return member;
    }

    public Long getMemberId(){
        return member.getMemberId();
    }

    public String getLoginId(){
        return member.getLoginId();
    }


    //사용자의 권한을 반환한다
    //Spring Security에서 hasRole("ADMIN") 같은 권한 검사를 사용할 때는
    //ROLE_ADMIN 형태의 권한명을 사용한다.
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + member.getRole().name())
        );
    }


    //DB에 저장된 암호화된 비밀번호를 반환한다.
    @Override
    public String getPassword(){
        return member.getPassword();
    }

    //username(로그인 ID)를 반환한다.
    @Override
    public String getUsername(){
        return member.getLoginId();
    }

    //계정 상태를 알려주는 메서드들

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !member.isDeleted();
    }


    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return !member.isDeleted();
    }
}
