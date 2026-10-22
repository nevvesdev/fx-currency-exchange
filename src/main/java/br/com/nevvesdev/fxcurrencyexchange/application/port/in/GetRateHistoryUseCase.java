package br.com.nevvesdev.fxcurrencyexchange.application.port.in;

import br.com.nevvesdev.fxcurrencyexchange.application.model.RateHistory;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;

import java.time.LocalDate;

public interface GetRateHistoryUseCase {

    RateHistory getHistory(CurrencyCode base, CurrencyCode quote, LocalDate from, LocalDate to);
}