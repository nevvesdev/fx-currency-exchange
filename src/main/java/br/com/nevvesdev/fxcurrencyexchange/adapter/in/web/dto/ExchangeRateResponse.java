package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto;

import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExchangeRateResponse(String base, String quote, String rate, Instant quotedAt, String source) {

    public static ExchangeRateResponse from(ExchangeRateLookup lookup) {
        return new ExchangeRateResponse(
                lookup.rate().base().value(),
                lookup.rate().quote().value(),
                lookup.rate().rate().toPlainString(),
                lookup.rate().quotedAt(),
                lookup.source().name());
    }
}