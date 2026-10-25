package com.aurawealth.plaid;

public class PlaidException extends RuntimeException {

	private final String errorCode;

	public PlaidException(String message) {
		this(message, (String) null);
	}

	/** {@code errorCode} is Plaid's {@code error_code} (e.g. INVALID_PUBLIC_TOKEN), or null if Plaid wasn't reached. */
	public PlaidException(String message, String errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public PlaidException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = null;
	}

	public String getErrorCode() {
		return errorCode;
	}

}
