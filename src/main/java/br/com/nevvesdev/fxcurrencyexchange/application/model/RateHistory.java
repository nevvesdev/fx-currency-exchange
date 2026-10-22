package br.com.nevvesdev.fxcurrencyexchange.application.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record RateHistory(
        CurrencyCode base, CurrencyCode quote, LocalDate from, LocalDate to, List<ExchangeRate> rates) {

    public RateHistory {
        Objects.requireNonNull(base, "base é obrigatória");
        Objects.requireNonNull(quote, "quote é obrigatória");
        Objects.requireNonNull(from, "from é obrigatória");
        Objects.requireNonNull(to, "to é obrigatória");
        rates = List.copyOf(rates);
    }
}