package br.com.nevvesdev.fxcurrencyexchange.application.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.util.Objects;

public record ExchangeRateLookup(ExchangeRate rate, RateSource source) {

    public ExchangeRateLookup {
        Objects.requireNonNull(rate, "rate é obrigatória");
        Objects.requireNonNull(source, "source é obrigatória");
    }
}