package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeteredExchangeRateProviderTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final ExchangeRate RATE =
            new ExchangeRate(USD, BRL, new BigDecimal("5.25"), Instant.parse("2026-10-20T00:00:00Z"));

    private SimpleMeterRegistry meterRegistry;
    private FakeProvider delegate;
    private MeteredExchangeRateProvider provider;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        delegate = new FakeProvider();
        provider = new MeteredExchangeRateProvider(delegate, meterRegistry, "frankfurter");
    }

    @Test
    @DisplayName("deve registrar chamada bem-sucedida de taxa única")
    void shouldRecordSuccessfulRateFetch() {
        assertThat(provider.fetchRate(USD, BRL)).isEqualTo(RATE);

        assertThat(count("rate", "success")).isEqualTo(1L);
    }

    @Test
    @DisplayName("deve registrar chamada bem-sucedida de todas as taxas da base")
    void shouldRecordSuccessfulBulkFetch() {
        assertThat(provider.fetchRates(USD)).containsExactly(RATE);

        assertThat(count("rates", "success")).isEqualTo(1L);
    }

    @Test
    @DisplayName("deve registrar taxa não encontrada e repropagar a exceção")
    void shouldRecordNotFound() {
        delegate.failure = new ExchangeRateNotFoundException("par inexistente");

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL)).isInstanceOf(ExchangeRateNotFoundException.class);

        assertThat(count("rate", "not_found")).isEqualTo(1L);
    }

    @Test
    @DisplayName("deve registrar falha do provedor e repropagar a exceção")
    void shouldRecordProviderError() {
        delegate.failure = new ExchangeRateProviderException("fora do ar");

        assertThatThrownBy(() -> provider.fetchRates(USD)).isInstanceOf(ExchangeRateProviderException.class);

        assertThat(count("rates", "error")).isEqualTo(1L);
    }

    private long count(String operation, String outcome) {
        return meterRegistry.get("fx.provider.requests")
                .tags("provider", "frankfurter", "operation", operation, "outcome", outcome)
                .timer()
                .count();
    }

    private static final class FakeProvider implements ExchangeRateProvider {

        private RuntimeException failure;

        @Override
        public ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote) {
            if (failure != null) {
                throw failure;
            }
            return RATE;
        }

        @Override
        public List<ExchangeRate> fetchRates(CurrencyCode base) {
            if (failure != null) {
                throw failure;
            }
            return List.of(RATE);
        }
    }
}