package com.salob.food_service.common.http_exceptions;

import com.salob.food_service.api._exceptions.EateryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalHttpExceptionHandler {
    @ExceptionHandler(EateryNotFoundException.class)
    public ResponseEntity<String> handleEateryNotFoundException(EateryNotFoundException ex) {
        String msg = "Eatery not found: " + ex.getMessage();
        log.error(msg);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        String msg = "An unexpected error occurred" + ex.getMessage();
        log.error(msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
    }
}
