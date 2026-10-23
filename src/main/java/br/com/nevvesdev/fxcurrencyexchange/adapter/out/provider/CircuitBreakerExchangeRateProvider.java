package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.List;
import java.util.function.Supplier;

public class CircuitBreakerExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerExchangeRateProvider(ExchangeRateProvider delegate, CircuitBreaker circuitBreaker) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote) {
        return execute(() -> delegate.fetchRate(base, quote));
    }

    @Override
    public List<ExchangeRate> fetchRates(CurrencyCode base) {
        return execute(() -> delegate.fetchRates(base));
    }

    private <T> T execute(Supplier<T> call) {
        try {
            return circuitBreaker.executeSupplier(call);
        } catch (CallNotPermittedException e) {
            throw new ExchangeRateProviderException("Circuit breaker aberto para o provedor de câmbio", e);
        }
    }
}