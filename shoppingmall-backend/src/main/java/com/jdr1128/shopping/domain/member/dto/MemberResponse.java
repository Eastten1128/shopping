package com.jdr1128.shopping.domain.member.dto;


import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemberResponse {
    private Long memberId;
    private String loginId;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MemberResponse(Long memberId,
                          String loginId,
                          String email,
                          Role role,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt
    ) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getLoginId(),
                member.getEmail(),
                member.getRole(),
                member.getCreatedAt(),
                member.getUpdateAt()
        );
    }

}
