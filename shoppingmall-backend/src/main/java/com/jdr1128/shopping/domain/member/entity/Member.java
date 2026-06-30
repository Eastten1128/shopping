package com.jdr1128.shopping.domain.member.entity;

import com.jdr1128.shopping.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter

public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "login_id")
    private String loginId;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleated = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Member(){

    }

    public Member(String loginId, String password, String email, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.role = role;
    }

}
