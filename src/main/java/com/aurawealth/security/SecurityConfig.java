package com.aurawealth.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			// Stateless bearer-token API: no cookies or sessions, so CSRF protection doesn't apply
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests((auth) -> auth
				.requestMatchers("/api/auth/**", "/error").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(Customizer.withDefaults()))
			.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtEncoder jwtEncoder(@Value("${aurawealth.jwt.secret}") String secret) {
		return NimbusJwtEncoder.withSecretKey(secretKey(secret)).build();
	}

	@Bean
	public JwtDecoder jwtDecoder(@Value("${aurawealth.jwt.secret}") String secret) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(secret))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.ISSUER));
		return decoder;
	}

	static SecretKey secretKey(String secret) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("aurawealth.jwt.secret (JWT_SECRET) must be at least 32 bytes for HS256");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

}
