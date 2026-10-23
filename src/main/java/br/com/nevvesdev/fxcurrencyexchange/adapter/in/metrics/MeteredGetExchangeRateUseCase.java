package br.com.nevvesdev.fxcurrencyexchange.adapter.in.metrics;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.DomainException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import io.micrometer.core.instrument.MeterRegistry;

public class MeteredGetExchangeRateUseCase implements GetExchangeRateUseCase {

    private final GetExchangeRateUseCase delegate;
    private final MeterRegistry meterRegistry;

    public MeteredGetExchangeRateUseCase(GetExchangeRateUseCase delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ExchangeRateLookup getRate(CurrencyCode base, CurrencyCode quote) {
        try {
            ExchangeRateLookup lookup = delegate.getRate(base, quote);
            meterRegistry.counter("fx.rate.lookups", "source", lookup.source().name()).increment();
            return lookup;
        } catch (ExchangeRateProviderException e) {
            countFailure("provider_unavailable");
            throw e;
        } catch (ExchangeRateNotFoundException e) {
            countFailure("not_found");
            throw e;
        } catch (DomainException e) {
            countFailure("invalid_request");
            throw e;
        }
    }

    private void countFailure(String reason) {
        meterRegistry.counter("fx.rate.lookup.failures", "reason", reason).increment();
    }
}