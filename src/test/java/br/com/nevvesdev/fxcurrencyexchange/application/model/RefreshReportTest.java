package br.com.nevvesdev.fxcurrencyexchange.application.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshReportTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");

    @Test
    @DisplayName("deve indicar sucesso quando nenhuma moeda falhou")
    void shouldBeSuccessWithoutFailures() {
        RefreshReport report = new RefreshReport(2, List.of(), 60, 5);

        assertThat(report.isSuccess()).isTrue();
        assertThat(report.isTotalFailure()).isFalse();
    }

    @Test
    @DisplayName("deve indicar falha parcial quando só algumas moedas falharam")
    void shouldBePartialFailure() {
        RefreshReport report = new RefreshReport(2, List.of(EUR), 30, 5);

        assertThat(report.isSuccess()).isFalse();
        assertThat(report.isTotalFailure()).isFalse();
    }

    @Test
    @DisplayName("deve indicar falha total quando todas as moedas falharam")
    void shouldBeTotalFailure() {
        RefreshReport report = new RefreshReport(2, List.of(USD, EUR), 0, 0);

        assertThat(report.isSuccess()).isFalse();
        assertThat(report.isTotalFailure()).isTrue();
    }

    @Test
    @DisplayName("não deve considerar falha total quando não havia moedas a atualizar")
    void shouldNotBeTotalFailureWhenNothingRequested() {
        RefreshReport report = new RefreshReport(0, List.of(), 0, 0);

        assertThat(report.isSuccess()).isTrue();
        assertThat(report.isTotalFailure()).isFalse();
    }

    @Test
    @DisplayName("deve manter cópia imutável da lista de falhas")
    void shouldKeepImmutableCopyOfFailures() {
        List<CurrencyCode> failures = new ArrayList<>(List.of(EUR));
        RefreshReport report = new RefreshReport(2, failures, 30, 5);

        failures.add(USD);

        assertThat(report.failedBases()).containsExactly(EUR);
        assertThatThrownBy(() -> report.failedBases().add(USD))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("deve rejeitar contadores negativos e falhas além do solicitado")
    void shouldRejectInvalidCounters() {
        assertThatThrownBy(() -> new RefreshReport(-1, List.of(), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshReport(1, List.of(), -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshReport(1, List.of(), 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshReport(1, List.of(USD, EUR), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deve rejeitar lista de falhas nula")
    void shouldRejectNullFailures() {
        assertThatThrownBy(() -> new RefreshReport(1, null, 0, 0))
                .isInstanceOf(NullPointerException.class);
    }
}