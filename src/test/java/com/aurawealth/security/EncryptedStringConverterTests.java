package com.aurawealth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class EncryptedStringConverterTests {

	private static final String KEY = "oZB1ICbmJcfTeLCDMSJuSE5zbs6aTpjXht3Tnc0M+Ns=";

	private static final String OTHER_KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

	private static final String TOKEN = "access-sandbox-de3ce8ef-33f8-452c-a685-8671031fc0f6";

	private final EncryptedStringConverter converter = new EncryptedStringConverter(KEY);

	@Test
	void roundTripsAndNeverStoresPlaintext() {
		String stored = converter.convertToDatabaseColumn(TOKEN);

		assertThat(stored).doesNotContain(TOKEN);
		assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(TOKEN);
	}

	@Test
	void sameInputEncryptsDifferentlyEachTime() {
		assertThat(converter.convertToDatabaseColumn(TOKEN)).isNotEqualTo(converter.convertToDatabaseColumn(TOKEN));
	}

	@Test
	void tamperedValueIsRejected() {
		byte[] bytes = Base64.getDecoder().decode(converter.convertToDatabaseColumn(TOKEN));
		bytes[bytes.length - 1] ^= 1;
		String tampered = Base64.getEncoder().encodeToString(bytes);

		assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered)).isInstanceOf(IllegalStateException.class)
			.hasMessageNotContaining(tampered);
	}

	@Test
	void valueEncryptedWithAnotherKeyIsRejected() {
		String stored = new EncryptedStringConverter(OTHER_KEY).convertToDatabaseColumn(TOKEN);

		assertThatThrownBy(() -> converter.convertToEntityAttribute(stored)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void nullPassesThrough() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
		assertThat(converter.convertToEntityAttribute(null)).isNull();
	}

	@Test
	void keyMustBeBase64Of32Bytes() {
		assertThatThrownBy(() -> new EncryptedStringConverter("not base64!")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new EncryptedStringConverter(Base64.getEncoder().encodeToString(new byte[16])))
			.isInstanceOf(IllegalStateException.class);
	}

}
