package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RateSource;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ExchangeRateQueryService implements GetExchangeRateUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateQueryService.class);

    private final ExchangeRateCache cache;
    private final ExchangeRateProvider provider;
    private final ExchangeRateRepository repository;

    public ExchangeRateQueryService(
            ExchangeRateCache cache, ExchangeRateProvider provider, ExchangeRateRepository repository) {
        this.cache = Objects.requireNonNull(cache, "cache é obrigatório");
        this.provider = Objects.requireNonNull(provider, "provider é obrigatório");
        this.repository = Objects.requireNonNull(repository, "repository é obrigatório");
    }

    @Override
    public ExchangeRateLookup getRate(CurrencyCode base, CurrencyCode quote) {
        Objects.requireNonNull(base, "base é obrigatória");
        Objects.requireNonNull(quote, "quote é obrigatória");
        if (base.equals(quote)) {
            throw new InvalidExchangeRateException("Moeda base e moeda cotada devem ser diferentes");
        }

        Optional<ExchangeRate> cached = cache.find(base, quote);
        if (cached.isPresent()) {
            return new ExchangeRateLookup(cached.get(), RateSource.CACHE);
        }

        ExchangeRate fresh;
        try {
            fresh = provider.fetchRate(base, quote);
        } catch (ExchangeRateProviderException e) {
            return fallbackToDatabase(base, quote, e);
        }

        repository.saveAll(List.of(fresh));
        cache.put(fresh);
        return new ExchangeRateLookup(fresh, RateSource.PROVIDER);
    }

    private ExchangeRateLookup fallbackToDatabase(
            CurrencyCode base, CurrencyCode quote, ExchangeRateProviderException failure) {
        return repository.findLatest(base, quote)
                .map(stale -> {
                    log.warn("Provedor indisponível para {}/{}; usando última taxa do banco ({})",
                            base, quote, stale.quotedAt());
                    return new ExchangeRateLookup(stale, RateSource.DATABASE);
                })
                .orElseThrow(() -> failure);
    }
}