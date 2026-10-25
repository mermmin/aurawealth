package com.aurawealth.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aurawealth.security.JwtService;
import com.aurawealth.user.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserService userService;

	private final JwtService jwtService;

	public AuthController(UserService userService, JwtService jwtService) {
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return AuthResponse.from(jwtService.issue(userService.register(request.email(), request.password(), request.displayName())));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return AuthResponse.from(jwtService.issue(userService.authenticate(request.email(), request.password())));
	}

}
