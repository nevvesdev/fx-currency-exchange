package br.com.nevvesdev.fxcurrencyexchange.application.port.out;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.util.List;

public interface ExchangeRateProvider {

    ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote);

    List<ExchangeRate> fetchRates(CurrencyCode base);
}