package com.jdr1128.shopping.domain.auth.service;

import com.jdr1128.shopping.domain.auth.entity.RefreshToken;
import com.jdr1128.shopping.domain.auth.exception.InvalidRefreshTokenException;
import com.jdr1128.shopping.domain.auth.exception.RefreshTokenNotFoundException;
import com.jdr1128.shopping.domain.auth.exception.RevokedRefreshTokenException;
import com.jdr1128.shopping.domain.auth.repository.RefreshTokenRepository;
import com.jdr1128.shopping.domain.member.dto.LoginResponse;
import com.jdr1128.shopping.domain.member.dto.LoginResult;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.global.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@Transactional(readOnly=true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     *
     * @param member
     * @return
     * 1. 기존 Refresh Token 행 삭제
     * 2. 새 Refresh Token 생성
     * 3. Refresh Token hash 생성
     * 4. DB에 hash 저장
     * 5. 원문 Refresh Token 반환
     *
     */

    //최초 로그인 시
    @Transactional
    public String issueRefreshToken(Member member){
        //기존 토큰을 실제 삭제하여 회원의 새 토큰만 남긴다.
        refreshTokenRepository.deleteAllByMemberId(member.getMemberId());

        //refresh Token 생성 및 저장
        return saveNewRefreshToken(member);
    }

    //RefreshToken Rotation
    @Transactional
    public LoginResult rotateRefreshToken(String rawRefreshToken){
        //Refresh Token의 존재확인
        if(!StringUtils.hasText(rawRefreshToken)) {
            throw new RefreshTokenNotFoundException();
        }

        //jwt 자체 검증
        if (!jwtTokenProvider.validateRefreshToken(rawRefreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        //원문 Refresh Token 해싱
        String tokenHash = hash(rawRefreshToken);

        //DB 조회 + 잠금
        RefreshToken currentRefreshToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        //폐기된 토큰인지 검증
        validateStoredRefreshToken(currentRefreshToken);

        //RefreshToken의 회원정보로 회원 담음
        Member member = currentRefreshToken.getMember();

        //탈퇴한 회원의 RefreshToken을 만들면 안됨
        if (member.isDeleted()){
            throw new InvalidRefreshTokenException();
        }

        //현재 토큰을 포함한 기존 행을 삭제하고 새 토큰으로 교체한다.
        refreshTokenRepository.deleteAllByMemberId(member.getMemberId());

        //새 Refresh Token 저장
        String newRefreshToken = saveNewRefreshToken(member);

        //새 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getMemberId(),
                member.getLoginId(),
                member.getRole()
        );

        //
        LoginResponse loginResponse = LoginResponse.of(
                newAccessToken,
                member.getMemberId(),
                member.getLoginId(),
                member.getRole()
        );

        return new LoginResult(loginResponse, newRefreshToken);
    }

    //로그아웃
    @Transactional
    public void logout(String rawRefreshToken){
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }

        String tokenHash = hash(rawRefreshToken);

        refreshTokenRepository.deleteByTokenHash(tokenHash);
    }

    // 재접속하지 않는 회원의 만료 토큰도 별도 트랜잭션에서 정리한다.
    @Transactional
    public int deleteExpiredAndRevokedTokens() {
        return refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
    }

    private String saveNewRefreshToken(Member member){
        String refreshToken = jwtTokenProvider.createRefreshToken(
                member.getMemberId(),
                member.getLoginId()
        );

        String tokenHash = hash(refreshToken);

        RefreshToken savedRefreshToken = new RefreshToken(
                member,
                tokenHash,
                jwtTokenProvider.getExpiration(refreshToken)
        );

        refreshTokenRepository.save(savedRefreshToken);

        return refreshToken;
    }
    //SHA-256 알고리즘으로 Refresh Token 변환하여 DB에 저장
    private String hash(String token){
        try{
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    //RefreshToken 검증
    private void validateStoredRefreshToken(RefreshToken refreshToken){
        //해당 refreshToken이 이미 폐기된 경우
        if (refreshToken.isRevoked()) {
            throw new RevokedRefreshTokenException();
        }
        //해당 refreshToken이 현재 기준 만료됐다면
        if (refreshToken.isExpired(LocalDateTime.now())){
            throw new InvalidRefreshTokenException();
        }
    }
}
