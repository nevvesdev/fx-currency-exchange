package br.com.nevvesdev.fxcurrencyexchange.adapter.in.scheduler;

import br.com.nevvesdev.fxcurrencyexchange.application.model.RefreshReport;
import br.com.nevvesdev.fxcurrencyexchange.application.port.in.RefreshExchangeRatesUseCase;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateRefreshJobTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");
    private static final FxRefreshProperties PROPERTIES = new FxRefreshProperties(List.of("USD", "EUR"));

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    @DisplayName("deve atualizar as moedas configuradas e registrar sucesso")
    void shouldRefreshConfiguredBasesAndRecordSuccess() {
        List<Collection<CurrencyCode>> received = new ArrayList<>();
        RefreshExchangeRatesUseCase useCase = bases -> {
            received.add(bases);
            return new RefreshReport(2, List.of(), 60, 5);
        };

        new ExchangeRateRefreshJob(useCase, PROPERTIES, meterRegistry).run();

        assertThat(received).hasSize(1);
        assertThat(received.get(0)).containsExactly(USD, EUR);
        assertThat(runs("success")).isEqualTo(1.0);
        assertThat(meterRegistry.get("fx.refresh.rates.inserted").counter().count()).isEqualTo(5.0);
        assertThat(meterRegistry.get("fx.refresh.duration").tag("outcome", "success").timer().count())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("deve registrar falha parcial quando só algumas moedas falham")
    void shouldRecordPartialOutcome() {
        RefreshExchangeRatesUseCase useCase = bases -> new RefreshReport(2, List.of(EUR), 30, 2);

        new ExchangeRateRefreshJob(useCase, PROPERTIES, meterRegistry).run();

        assertThat(runs("partial")).isEqualTo(1.0);
        assertThat(meterRegistry.get("fx.refresh.duration").tag("outcome", "partial").timer().count())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("deve registrar falha quando todas as moedas falham")
    void shouldRecordFailureOutcomeOnTotalFailure() {
        RefreshExchangeRatesUseCase useCase = bases -> new RefreshReport(2, List.of(USD, EUR), 0, 0);

        new ExchangeRateRefreshJob(useCase, PROPERTIES, meterRegistry).run();

        assertThat(runs("failure")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve registrar falha e propagar a exceção quando o caso de uso quebra")
    void shouldRecordFailureAndRethrowOnException() {
        RefreshExchangeRatesUseCase useCase = bases -> {
            throw new IllegalStateException("banco indisponível");
        };
        ExchangeRateRefreshJob job = new ExchangeRateRefreshJob(useCase, PROPERTIES, meterRegistry);

        assertThatThrownBy(job::run).isInstanceOf(IllegalStateException.class);

        assertThat(runs("failure")).isEqualTo(1.0);
        assertThat(meterRegistry.get("fx.refresh.duration").tag("outcome", "failure").timer().count())
                .isEqualTo(1L);
        assertThat(meterRegistry.find("fx.refresh.rates.inserted").counter()).isNull();
    }

    private double runs(String outcome) {
        return meterRegistry.get("fx.refresh.runs").tag("outcome", outcome).counter().count();
    }
}