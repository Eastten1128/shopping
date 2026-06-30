package com.jdr1128.shopping.domain.member.repository;

import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원을 저장하고 ID로 조회할 수 있다.")
    void saveAndFindMemberById() {
        //given
        Member member = new Member(
                "user01",
                "encoded-password",
                "user01@test.com",
                Role.USER
        );
        Member savedMember = memberRepository.save(member);
        //when
        Member foundMember = memberRepository.findById(savedMember.getMemberId())
                .orElseThrow();

        //then
        assertThat(foundMember.getLoginId()).isEqualTo("user01");
        assertThat(foundMember.getEmail()).isEqualTo("user01@test.com");
        assertThat(foundMember.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("loginId로 회원을 조회할 수 있다.")
    void findByLoginId(){
        //given
        Member member = new Member(
                "seller01",
                "encoded-password",
                "seller01@test.com",
                Role.SELLER
        );

        memberRepository.save(member);

        //when
        Member foundMember = memberRepository.findByLoginId("seller01")
                .orElseThrow();

        //Then
        assertThat(foundMember.getLoginId()).isEqualTo("seller01");
        assertThat(foundMember.getEmail()).isEqualTo("seller01@test.com");
    }

    @Test
    @DisplayName("email 중복 여부를 확인할 수 있다.")
    void existsByEmail(){
        //given

        Member member = new Member(
                "admin01",
                "encoded-password",
                "admin01@test.com",
                Role.ADMIN
        );

        memberRepository.save(member);

        //when
        boolean exists = memberRepository.existsByEmail("admin01@test.com");

        //then
        assertThat(exists).isTrue();
    }
}
