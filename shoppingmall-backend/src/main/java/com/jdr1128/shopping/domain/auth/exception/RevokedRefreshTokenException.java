package com.jdr1128.shopping.domain.auth.exception;

public class RevokedRefreshTokenException extends RuntimeException {
  public RevokedRefreshTokenException(String message) {
    super(message);
  }
}
