package br.com.nevvesdev.fxcurrencyexchange.application.port.in;

import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;

public interface GetExchangeRateUseCase {

    ExchangeRateLookup getRate(CurrencyCode base, CurrencyCode quote);
}