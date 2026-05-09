package com.salob.user_service.common.http_exceptions;

import com.auth0.jwt.exceptions.JWTCreationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalHttpExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        String msg = "An unexpected error occurred" + ex.getMessage();
        log.error(msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
    }

//    @ExceptionHandler(JWTCreationException.class)
//    public ResponseEntity<String> handleJwtException(Exception ex) {
//        String msg = "Error creating JWT token: " + ex.getMessage();
//        log.error(msg);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
//    }
//
//    @ExceptionHandler(JWTCreationException.class)
//    public ResponseEntity<String> handleJwtException(Exception ex) {
//        String msg = "Error creating JWT token: " + ex.getMessage();
//        log.error(msg);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
//    }
}
