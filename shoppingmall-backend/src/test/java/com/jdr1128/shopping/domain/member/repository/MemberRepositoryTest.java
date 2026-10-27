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
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원을 저장하고 ID로 조회할 수 있다.")
    void saveAndFindMemberById(){
        //given

        Member member = new Member(
                "user99",
                "password",
                "user99@test.com",
                Role.USER
        );

        Member savedMember = memberRepository.save(member);
        //when

        Member foundMember = memberRepository.findById(savedMember.getMemberId())
                        .orElseThrow();
        //then
        assertThat(foundMember.getLoginId()).isEqualTo("user99");
        assertThat(foundMember.getEmail()).isEqualTo("user99@test.com");
        assertThat(foundMember.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("loginId로 회원을 조회할 수 있다")
    void findByLoginId(){
        //given

        String sellerLoginId = "seller99";
        Member member = new Member(
                sellerLoginId,
                "encoded-password",
                "seller99@test.com",
                Role.SELLER
        );

        Member savedMember = memberRepository.save(member);

        //when
        Member foundMember = memberRepository.findByLoginId(sellerLoginId)
                .orElseThrow();

        //then
        assertThat(foundMember.getLoginId()).isEqualTo("seller99");
        assertThat(foundMember.getRole()).isEqualTo(Role.SELLER);
    }

    @Test
    @DisplayName("email 중복 여부를 확인할 수 있다.")
    void existsByEmail(){
        //given
        Member member1 = new Member(
                "user99",
                "password",
                "user99@test.com",
                Role.USER
        );

        Member savedMember1 = memberRepository.save(member1);

        //when
        boolean exists = memberRepository.existsByEmail("user99@test.com");

        //then

        assertThat(exists).isTrue();


    }


}
