package br.com.nevvesdev.fxcurrencyexchange.application.port.out;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.util.Optional;

public interface ExchangeRateCache {

    Optional<ExchangeRate> find(CurrencyCode base, CurrencyCode quote);

    void put(ExchangeRate rate);
}