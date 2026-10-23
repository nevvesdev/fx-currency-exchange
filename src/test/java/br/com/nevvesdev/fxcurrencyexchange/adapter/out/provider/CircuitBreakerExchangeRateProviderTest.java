package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerExchangeRateProviderTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final ExchangeRate RATE =
            new ExchangeRate(USD, BRL, new BigDecimal("5.25"), Instant.parse("2026-10-20T00:00:00Z"));

    private FakeProvider delegate;
    private CircuitBreaker circuitBreaker;
    private CircuitBreakerExchangeRateProvider provider;

    @BeforeEach
    void setUp() {
        delegate = new FakeProvider();
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofHours(1))
                .recordExceptions(ExchangeRateProviderException.class)
                .build());
        provider = new CircuitBreakerExchangeRateProvider(delegate, circuitBreaker);
    }

    @Test
    @DisplayName("deve repassar o resultado do provedor com o circuito fechado")
    void shouldPassThroughSuccess() {
        assertThat(provider.fetchRate(USD, BRL)).isEqualTo(RATE);
        assertThat(provider.fetchRates(USD)).containsExactly(RATE);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("deve abrir o circuito após falhas e rejeitar chamadas sem acionar o provedor")
    void shouldOpenAfterFailuresAndFailFast() {
        delegate.failure = new ExchangeRateProviderException("fora do ar");

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL)).isInstanceOf(ExchangeRateProviderException.class);
        assertThatThrownBy(() -> provider.fetchRate(USD, BRL)).isInstanceOf(ExchangeRateProviderException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                .isInstanceOf(ExchangeRateProviderException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);
        assertThat(delegate.calls).isEqualTo(2);
    }

    @Test
    @DisplayName("deve proteger também a busca de todas as taxas com o mesmo circuito")
    void shouldGuardBulkFetchWithSameCircuit() {
        delegate.failure = new ExchangeRateProviderException("fora do ar");
        assertThatThrownBy(() -> provider.fetchRate(USD, BRL)).isInstanceOf(ExchangeRateProviderException.class);
        assertThatThrownBy(() -> provider.fetchRate(USD, BRL)).isInstanceOf(ExchangeRateProviderException.class);

        assertThatThrownBy(() -> provider.fetchRates(USD))
                .isInstanceOf(ExchangeRateProviderException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);
        assertThat(delegate.calls).isEqualTo(2);
    }

    @Test
    @DisplayName("não deve contar taxa não encontrada como falha do provedor")
    void shouldNotCountNotFoundAsFailure() {
        delegate.failure = new ExchangeRateNotFoundException("par inexistente");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                    .isInstanceOf(ExchangeRateNotFoundException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        delegate.failure = null;
        assertThat(provider.fetchRate(USD, BRL)).isEqualTo(RATE);
        assertThat(delegate.calls).isEqualTo(6);
    }

    private static final class FakeProvider implements ExchangeRateProvider {

        private RuntimeException failure;
        private int calls;

        @Override
        public ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return RATE;
        }

        @Override
        public List<ExchangeRate> fetchRates(CurrencyCode base) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return List.of(RATE);
        }
    }
}