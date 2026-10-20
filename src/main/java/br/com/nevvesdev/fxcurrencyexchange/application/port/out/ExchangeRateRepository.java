package br.com.nevvesdev.fxcurrencyexchange.application.port.out;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository {

    int saveAll(Collection<ExchangeRate> rates);

    Optional<ExchangeRate> findLatest(CurrencyCode base, CurrencyCode quote);

    List<ExchangeRate> findHistory(CurrencyCode base, CurrencyCode quote, Instant from, Instant to);
}