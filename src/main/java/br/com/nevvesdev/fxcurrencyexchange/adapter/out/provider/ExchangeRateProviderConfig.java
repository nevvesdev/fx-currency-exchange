package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider;

import br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter.FrankfurterExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FxCircuitBreakerProperties.class)
class ExchangeRateProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateProviderConfig.class);
    private static final String PROVIDER_NAME = "frankfurter";

    @Bean
    ExchangeRateProvider exchangeRateProvider(
            @Qualifier("frankfurterRestClient") RestClient restClient,
            FxCircuitBreakerProperties properties,
            MeterRegistry meterRegistry) {
        CircuitBreaker circuitBreaker = CircuitBreaker.of(PROVIDER_NAME, circuitBreakerConfig(properties));
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Circuit breaker do provedor {} mudou de estado: {}",
                        PROVIDER_NAME, event.getStateTransition()));
        Gauge.builder("fx.provider.circuit.open", circuitBreaker,
                        breaker -> breaker.getState() == CircuitBreaker.State.OPEN ? 1.0 : 0.0)
                .tag("provider", PROVIDER_NAME)
                .register(meterRegistry);

        ExchangeRateProvider frankfurter = new FrankfurterExchangeRateProvider(restClient);
        ExchangeRateProvider protectedProvider = new CircuitBreakerExchangeRateProvider(frankfurter, circuitBreaker);
        return new MeteredExchangeRateProvider(protectedProvider, meterRegistry, PROVIDER_NAME);
    }

    private static CircuitBreakerConfig circuitBreakerConfig(FxCircuitBreakerProperties properties) {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(properties.slidingWindowSize())
                .minimumNumberOfCalls(properties.minimumCalls())
                .failureRateThreshold(properties.failureRateThreshold())
                .waitDurationInOpenState(properties.waitInOpenState())
                .permittedNumberOfCallsInHalfOpenState(properties.halfOpenCalls())
                .recordExceptions(ExchangeRateProviderException.class)
                .build();
    }
}