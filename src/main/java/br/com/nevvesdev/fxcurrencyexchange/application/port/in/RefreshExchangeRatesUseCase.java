package br.com.nevvesdev.fxcurrencyexchange.application.port.in;

import br.com.nevvesdev.fxcurrencyexchange.application.model.RefreshReport;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;

import java.util.Collection;

public interface RefreshExchangeRatesUseCase {

    RefreshReport refresh(Collection<CurrencyCode> bases);
}