package com.aurawealth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.aurawealth.user.EmailAlreadyRegisteredException;

/**
 * Turns domain exceptions into RFC 9457 problem responses. Extending
 * {@link ResponseEntityExceptionHandler} also renders validation failures as 400 problem bodies.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	public ProblemDetail handleEmailAlreadyRegistered() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email is already registered");
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ProblemDetail handleBadCredentials() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

}
