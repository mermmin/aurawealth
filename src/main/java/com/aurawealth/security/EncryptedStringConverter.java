package com.aurawealth.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Encrypts a String column at rest with AES-256-GCM. The stored value is base64(iv || ciphertext+tag), with a fresh
 * random IV per write, so equal plaintexts never produce equal column values and any tampering fails decryption.
 * Apply per field with {@code @Convert(converter = EncryptedStringConverter.class)}.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private static final int IV_BYTES = 12;

	private static final int TAG_BITS = 128;

	private final SecretKey key;

	private final SecureRandom random = new SecureRandom();

	public EncryptedStringConverter(@Value("${aurawealth.crypto.token-key}") String base64Key) {
		this.key = aesKey(base64Key);
	}

	@Override
	public String convertToDatabaseColumn(String plaintext) {
		if (plaintext == null) {
			return null;
		}
		try {
			byte[] iv = new byte[IV_BYTES];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder()
				.encodeToString(ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Could not encrypt column value", ex);
		}
	}

	@Override
	public String convertToEntityAttribute(String stored) {
		if (stored == null) {
			return null;
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(stored);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));
			byte[] plaintext = cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES);
			return new String(plaintext, StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException | IllegalArgumentException ex) {
			// Wrong key, tampered data, or not our format; never include the stored value in the message
			throw new IllegalStateException("Could not decrypt column value", ex);
		}
	}

	static SecretKey aesKey(String base64Key) {
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(base64Key);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException("aurawealth.crypto.token-key (TOKEN_ENCRYPTION_KEY) must be base64", ex);
		}
		if (bytes.length != 32) {
			throw new IllegalStateException(
					"aurawealth.crypto.token-key (TOKEN_ENCRYPTION_KEY) must decode to 32 bytes for AES-256");
		}
		return new SecretKeySpec(bytes, "AES");
	}

}
