package com.salob.user_service.common.http_exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

@Slf4j
@RestControllerAdvice
public class GlobalHttpExceptionHandler {
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleAllExceptions(Exception ex, HandlerMethod handler) {
		String methodName = handler.getMethod().getName();
		String controllerName = handler.getBeanType().getSimpleName();
		log.error("{}#{} — {}", controllerName, methodName, ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex, HandlerMethod handler) {
		String methodName = handler.getMethod().getName();
		String controllerName = handler.getBeanType().getSimpleName();
		log.warn("{}#{} — Missing required header: {}", controllerName, methodName, ex.getHeaderName());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing required header: " + ex.getHeaderName());
	}
}
