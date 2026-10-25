package com.aurawealth.plaid;

import java.time.OffsetDateTime;

public record LinkTokenResponse(String linkToken, OffsetDateTime expiration) {
}
