package com.jdr1128.shopping.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 *  전역 예외 처리 클래스
 *
 */

//모든 컨트롤러에서 발생하는 예외를 공통으로 처리할 수 있도록 하는 어노테이션
@RestControllerAdvice
public class GlobalExceptionHandler {

    //지정한 예외가 발생했을 때 어떤 메서드를 실행할지 SPRING에 알려주는 어노테이션
    //BusinessException 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e
    ){
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    //MethodArgumentNotValidException 처리
    //DTO의 입력값 검증이 실패했을 때 발생하는 예외를 처리한다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        //getBindingResult: Spring Framework에서 데이터 유효성 검증(@Valid 또는 @Validated)
        //실패 시 발생하는 예외 객체에서 검증 오류 정보를 담고 있는 BindingResult를 꺼내는 메서드
        List<ErrorResponse.ValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        //공통 에러 코드 지정
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errors));
    }

    //요청 URL은 존재하지만 지원하지 않는 HTTP Method로 요청했을 때 발생하는 예외 처리
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHtppRequestMethodSupportedException(
            HttpRequestMethodNotSupportedException e
    ) {
        //errorCode - 허용되지 않은 메소드
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;

        //
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    //앞의 예외 처리 메서드에서 처리하지 못한 모든 에외를 최종적으로 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e
    ){
        //500 - 클라이언트에는 내부 예외 메시지나 스택 트레이스를 직접 전달하지 않고 공통 메시지 반환
        //DB 정보, 파일 경로, 내부 클래스명처럼 외부에 노출하면 안되는 서버 정보가 응답에 포함되는 것을 방지
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    //Spring의 FieldError 객체를 프로젝트에 정의한 ValidationError 객체로 변환하는 보조 메서드
    private ErrorResponse.ValidationError toValidationError(FieldError fieldError){
        return ErrorResponse.ValidationError.of(
                fieldError.getField(), //오류가 난 필드명
                fieldError.getDefaultMessage() //출력할 오류 안내 문구
        );
    }
}
