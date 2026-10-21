package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RefreshReport;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.RefreshExchangeRatesUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ExchangeRateRefreshService implements RefreshExchangeRatesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateRefreshService.class);

    private final ExchangeRateProvider provider;
    private final ExchangeRateRepository repository;
    private final ExchangeRateCache cache;

    public ExchangeRateRefreshService(
            ExchangeRateProvider provider, ExchangeRateRepository repository, ExchangeRateCache cache) {
        this.provider = Objects.requireNonNull(provider, "provider é obrigatório");
        this.repository = Objects.requireNonNull(repository, "repository é obrigatório");
        this.cache = Objects.requireNonNull(cache, "cache é obrigatório");
    }

    @Override
    public RefreshReport refresh(Collection<CurrencyCode> bases) {
        Objects.requireNonNull(bases, "bases é obrigatório");
        Set<CurrencyCode> distinctBases = new LinkedHashSet<>(bases);
        List<CurrencyCode> failedBases = new ArrayList<>();
        int fetched = 0;
        int inserted = 0;

        for (CurrencyCode base : distinctBases) {
            try {
                List<ExchangeRate> rates = provider.fetchRates(base);
                inserted += repository.saveAll(rates);
                cache.putAll(rates);
                fetched += rates.size();
            } catch (ExchangeRateProviderException | ExchangeRateNotFoundException e) {
                log.warn("Falha ao atualizar as taxas da moeda base {}: {}", base, e.getMessage());
                failedBases.add(base);
            }
        }

        return new RefreshReport(distinctBases.size(), failedBases, fetched, inserted);
    }
}