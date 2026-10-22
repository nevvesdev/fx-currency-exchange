package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web.dto;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.time.Instant;

public record RatePointResponse(Instant quotedAt, String rate) {

    public static RatePointResponse from(ExchangeRate rate) {
        return new RatePointResponse(rate.quotedAt(), rate.rate().toPlainString());
    }
}