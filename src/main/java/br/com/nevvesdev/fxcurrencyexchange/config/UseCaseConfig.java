package br.com.nevvesdev.fxcurrencyexchange.config;

import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.application.service.ExchangeRateQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class UseCaseConfig {

    @Bean
    GetExchangeRateUseCase getExchangeRateUseCase(
            ExchangeRateCache cache, ExchangeRateProvider provider, ExchangeRateRepository repository) {
        return new ExchangeRateQueryService(cache, provider, repository);
    }
}