package com.aurawealth.plaid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicTokenExchangeRequest(@NotBlank @Size(max = 255) String publicToken) {
}
