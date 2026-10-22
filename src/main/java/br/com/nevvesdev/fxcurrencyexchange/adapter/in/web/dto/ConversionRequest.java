package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConversionRequest(
        @NotBlank(message = "O campo from é obrigatório") String from,
        @NotBlank(message = "O campo to é obrigatório") String to,
        @NotNull(message = "O campo amount é obrigatório")
        @Positive(message = "O campo amount deve ser maior que zero") BigDecimal amount) {
}