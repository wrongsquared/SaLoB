package com.salob.food_service.common.http_exceptions;

import com.salob.food_service.api._exceptions.EateryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

@Slf4j
@RestControllerAdvice
public class GlobalHttpExceptionHandler {
	@ExceptionHandler(EateryNotFoundException.class)
	public ResponseEntity<String> handleEateryNotFoundException(EateryNotFoundException ex, HandlerMethod handler) {
		String methodName = handler.getMethod().getName();
		String controllerName = handler.getBeanType().getSimpleName();
		log.warn("{}#{} — Eatery not found: {}", controllerName, methodName, ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Eatery not found");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleAllExceptions(Exception ex, HandlerMethod handler) {
		String methodName = handler.getMethod().getName();
		String controllerName = handler.getBeanType().getSimpleName();
		log.error("{}#{} — {}", controllerName, methodName, ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
	}
}
