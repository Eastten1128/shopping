package com.jdr1128.shopping.domain.auth;

public class RefreshTokenNotFoundException extends RuntimeException {
  public RefreshTokenNotFoundException(String message) {
    super(message);
  }
}
