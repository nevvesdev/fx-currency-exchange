package br.com.nevvesdev.fxcurrencyexchange.config;

import br.com.nevvesdev.fxcurrencyexchange.adapter.in.metrics.MeteredGetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.ConvertCurrencyUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetRateHistoryUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.QueryConversionsUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.RefreshExchangeRatesUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ConversionRepository;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.application.service.ConversionQueryService;
import br.com.nevvesdev.fxcurrencyexchange.application.service.ConvertCurrencyService;
import br.com.nevvesdev.fxcurrencyexchange.application.service.ExchangeRateQueryService;
import br.com.nevvesdev.fxcurrencyexchange.application.service.ExchangeRateRefreshService;
import br.com.nevvesdev.fxcurrencyexchange.application.service.RateHistoryService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class UseCaseConfig {

    @Bean
    GetExchangeRateUseCase getExchangeRateUseCase(
            ExchangeRateCache cache,
            ExchangeRateProvider provider,
            ExchangeRateRepository repository,
            MeterRegistry meterRegistry) {
        return new MeteredGetExchangeRateUseCase(
                new ExchangeRateQueryService(cache, provider, repository), meterRegistry);
    }

    @Bean
    RefreshExchangeRatesUseCase refreshExchangeRatesUseCase(
            ExchangeRateProvider provider, ExchangeRateRepository repository, ExchangeRateCache cache) {
        return new ExchangeRateRefreshService(provider, repository, cache);
    }

    @Bean
    GetRateHistoryUseCase getRateHistoryUseCase(ExchangeRateRepository repository, Clock clock) {
        return new RateHistoryService(repository, clock);
    }

    @Bean
    ConvertCurrencyUseCase convertCurrencyUseCase(
            GetExchangeRateUseCase exchangeRates, ConversionRepository conversions, Clock clock) {
        return new ConvertCurrencyService(exchangeRates, conversions, clock);
    }

    @Bean
    QueryConversionsUseCase queryConversionsUseCase(ConversionRepository conversions) {
        return new ConversionQueryService(conversions);
    }
}