package br.com.nevvesdev.fxcurrencyexchange.adapter.in.metrics;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RateSource;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.GetExchangeRateUseCase;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeteredGetExchangeRateUseCaseTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final ExchangeRate RATE =
            new ExchangeRate(USD, BRL, new BigDecimal("5.25"), Instant.parse("2026-10-20T00:00:00Z"));

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    @DisplayName("deve contar cada consulta bem-sucedida por origem do dado")
    void shouldCountLookupsBySource() {
        RateSource[] sources = {RateSource.CACHE, RateSource.CACHE, RateSource.PROVIDER, RateSource.DATABASE};
        int[] index = {0};
        GetExchangeRateUseCase delegate = (base, quote) -> new ExchangeRateLookup(RATE, sources[index[0]++]);
        MeteredGetExchangeRateUseCase useCase = new MeteredGetExchangeRateUseCase(delegate, meterRegistry);

        for (int call = 0; call < sources.length; call++) {
            assertThat(useCase.getRate(USD, BRL).rate()).isEqualTo(RATE);
        }

        assertThat(lookups("CACHE")).isEqualTo(2.0);
        assertThat(lookups("PROVIDER")).isEqualTo(1.0);
        assertThat(lookups("DATABASE")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve contar falha do provedor e repropagar a exceção")
    void shouldCountProviderFailure() {
        GetExchangeRateUseCase delegate = (base, quote) -> {
            throw new ExchangeRateProviderException("fora do ar");
        };
        MeteredGetExchangeRateUseCase useCase = new MeteredGetExchangeRateUseCase(delegate, meterRegistry);

        assertThatThrownBy(() -> useCase.getRate(USD, BRL)).isInstanceOf(ExchangeRateProviderException.class);

        assertThat(failures("provider_unavailable")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve contar taxa não encontrada e repropagar a exceção")
    void shouldCountNotFound() {
        GetExchangeRateUseCase delegate = (base, quote) -> {
            throw new ExchangeRateNotFoundException("par inexistente");
        };
        MeteredGetExchangeRateUseCase useCase = new MeteredGetExchangeRateUseCase(delegate, meterRegistry);

        assertThatThrownBy(() -> useCase.getRate(USD, BRL)).isInstanceOf(ExchangeRateNotFoundException.class);

        assertThat(failures("not_found")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve contar requisição inválida e repropagar a exceção de domínio")
    void shouldCountInvalidRequest() {
        GetExchangeRateUseCase delegate = (base, quote) -> {
            throw new InvalidExchangeRateException("Moeda base e moeda cotada devem ser diferentes");
        };
        MeteredGetExchangeRateUseCase useCase = new MeteredGetExchangeRateUseCase(delegate, meterRegistry);

        assertThatThrownBy(() -> useCase.getRate(USD, USD)).isInstanceOf(InvalidExchangeRateException.class);

        assertThat(failures("invalid_request")).isEqualTo(1.0);
    }

    private double lookups(String source) {
        return meterRegistry.get("fx.rate.lookups").tag("source", source).counter().count();
    }

    private double failures(String reason) {
        return meterRegistry.get("fx.rate.lookup.failures").tag("reason", reason).counter().count();
    }
}