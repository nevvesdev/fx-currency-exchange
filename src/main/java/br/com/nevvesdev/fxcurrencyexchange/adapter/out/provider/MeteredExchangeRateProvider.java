package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.List;
import java.util.function.Supplier;

public class MeteredExchangeRateProvider implements ExchangeRateProvider {

    private static final String METRIC_NAME = "fx.provider.requests";

    private final ExchangeRateProvider delegate;
    private final MeterRegistry meterRegistry;
    private final String providerName;

    public MeteredExchangeRateProvider(
            ExchangeRateProvider delegate, MeterRegistry meterRegistry, String providerName) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
        this.providerName = providerName;
    }

    @Override
    public ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote) {
        return record("rate", () -> delegate.fetchRate(base, quote));
    }

    @Override
    public List<ExchangeRate> fetchRates(CurrencyCode base) {
        return record("rates", () -> delegate.fetchRates(base));
    }

    private <T> T record(String operation, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        try {
            T result = call.get();
            outcome = "success";
            return result;
        } catch (ExchangeRateNotFoundException e) {
            outcome = "not_found";
            throw e;
        } finally {
            sample.stop(Timer.builder(METRIC_NAME)
                    .tag("provider", providerName)
                    .tag("operation", operation)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }
}