package com.aurawealth.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// BCrypt only uses the first 72 bytes of a password, hence the upper bound. displayName is optional.
public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) String password, @Size(max = 100) String displayName) {
}
