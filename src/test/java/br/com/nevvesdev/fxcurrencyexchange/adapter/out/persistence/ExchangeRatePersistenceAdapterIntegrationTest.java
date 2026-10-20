package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import br.com.nevvesdev.fxcurrencyexchange.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ExchangeRatePersistenceAdapterIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");
    private static final Instant DAY_1 = Instant.parse("2026-10-18T00:00:00Z");
    private static final Instant DAY_2 = Instant.parse("2026-10-19T00:00:00Z");
    private static final Instant DAY_3 = Instant.parse("2026-10-20T00:00:00Z");

    @Autowired
    private ExchangeRateRepository repository;

    @Test
    @DisplayName("deve persistir taxas e informar quantas foram inseridas")
    void shouldPersistRatesAndReportInsertedCount() {
        int inserted = repository.saveAll(List.of(
                rate(USD, BRL, "5.40", DAY_1),
                rate(USD, EUR, "0.89166", DAY_1)));

        assertThat(inserted).isEqualTo(2);
    }

    @Test
    @DisplayName("deve ignorar taxas repetidas para o mesmo par e instante de cotação")
    void shouldIgnoreDuplicatedRates() {
        List<ExchangeRate> rates = List.of(rate(USD, BRL, "5.40", DAY_1));

        int first = repository.saveAll(rates);
        int second = repository.saveAll(rates);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(repository.findHistory(USD, BRL, DAY_1, DAY_3)).hasSize(1);
    }

    @Test
    @DisplayName("deve retornar a taxa de cotação mais recente do par")
    void shouldFindLatestRate() {
        repository.saveAll(List.of(
                rate(USD, BRL, "5.40", DAY_1),
                rate(USD, BRL, "5.50", DAY_3),
                rate(USD, BRL, "5.45", DAY_2)));

        assertThat(repository.findLatest(USD, BRL)).contains(rate(USD, BRL, "5.5", DAY_3));
    }

    @Test
    @DisplayName("deve retornar vazio quando não há taxa para o par")
    void shouldReturnEmptyWhenNoRate() {
        assertThat(repository.findLatest(USD, BRL)).isEmpty();
    }

    @Test
    @DisplayName("deve manter os pares de moedas isolados entre si")
    void shouldKeepPairsIsolated() {
        repository.saveAll(List.of(
                rate(USD, BRL, "5.40", DAY_1),
                rate(USD, EUR, "0.89166", DAY_2)));

        assertThat(repository.findLatest(USD, EUR)).contains(rate(USD, EUR, "0.89166", DAY_2));
        assertThat(repository.findLatest(USD, BRL)).contains(rate(USD, BRL, "5.40", DAY_1));
    }

    @Test
    @DisplayName("deve listar o histórico no intervalo, inclusivo, do mais recente para o mais antigo")
    void shouldListHistoryInRangeNewestFirst() {
        repository.saveAll(List.of(
                rate(USD, BRL, "5.40", DAY_1),
                rate(USD, BRL, "5.45", DAY_2),
                rate(USD, BRL, "5.50", DAY_3)));

        List<ExchangeRate> history = repository.findHistory(USD, BRL, DAY_1, DAY_2);

        assertThat(history).extracting(ExchangeRate::quotedAt).containsExactly(DAY_2, DAY_1);
    }

    @Test
    @DisplayName("deve preservar a precisão da taxa ao ler do banco")
    void shouldPreserveRatePrecision() {
        ExchangeRate precise = rate(USD, BRL, "5.43210987", DAY_1);
        repository.saveAll(List.of(precise));

        assertThat(repository.findLatest(USD, BRL)).contains(precise);
    }

    private static ExchangeRate rate(CurrencyCode base, CurrencyCode quote, String value, Instant quotedAt) {
        return new ExchangeRate(base, quote, new BigDecimal(value), quotedAt);
    }
}