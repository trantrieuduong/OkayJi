package com.okayji.exception;

import com.okayji.common.ApiResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j(topic = "GLOBAL-EXCEPTION-HANDLER")
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";
    private static final String MAX_ATTRIBUTE = "max";

    /*
     * Unhandled errors during runtime
     */
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.builder()
                        .success(false)
                        .message(AppError.UNCATEGORIZED_EXCEPTION.getMessage())
                        .build());
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getErrorCode().getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getErrorCode().getMessage())
                        .build());
    }

    /*
     * Exception from annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String enumKey = ex.getFieldError().getDefaultMessage();
        AppError appError;
        Map<String, Object> attributes = null;

        try {
            appError = AppError.valueOf(enumKey);

            var constraintViolation = ex.getBindingResult().getAllErrors()
                    .getFirst().unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();
        }
        catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            appError = AppError.UNCATEGORIZED_EXCEPTION;
        }

        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .message(Objects.nonNull(attributes)
                                ? mapAttribute(appError.getMessage(), attributes)
                                : appError.getMessage())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException ex) {
        AppError appError = AppError.UNAUTHORIZED;

        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .message(appError.getMessage())
                        .build());
    }

    @ExceptionHandler(JwtException.class)
    ResponseEntity<ApiResponse> handleJwtException(JwtException ex) {
        AppError appError = AppError.UNAUTHENTICATED;

        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .message(appError.getMessage())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        AppError appError = AppError.UNCATEGORIZED_EXCEPTION;

        if (cause instanceof ConstraintViolationException cve) {
            String constraint = cve.getConstraintName();

            switch(constraint) {
                case "user.uk_user_username" -> appError = AppError.USERNAME_EXISTED;
                case "user.uk_user_email" -> appError = AppError.EMAIL_EXISTED;
                case "friend_request.uk_fr_pair" -> appError = AppError.FRIEND_REQUEST_EXISTS;
                case "friend.uk_friends_pair" -> appError = AppError.FRIEND_ALREADY;
            }
        }
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .message(appError.getMessage())
                        .build());
    }

    /*
     * Issues with the request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        AppError appError = AppError.INVALID_INPUT_DATA;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .message(appError.getMessage())
                        .build());
    }

    private String mapAttribute(String message, Map<String, Object> attributes){
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        String maxValue = String.valueOf(attributes.get(MAX_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue)
                .replace("{" + MAX_ATTRIBUTE + "}", maxValue);
    }
}
