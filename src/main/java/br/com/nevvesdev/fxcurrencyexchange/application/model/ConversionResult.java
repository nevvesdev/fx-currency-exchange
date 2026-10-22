package br.com.nevvesdev.fxcurrencyexchange.application.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;

import java.util.Objects;

public record ConversionResult(Conversion conversion, RateSource rateSource) {

    public ConversionResult {
        Objects.requireNonNull(conversion, "conversion é obrigatória");
        Objects.requireNonNull(rateSource, "rateSource é obrigatória");
    }
}