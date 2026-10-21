package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RefreshReport;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateRefreshServiceTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final ExchangeRate USD_BRL = rate(USD, BRL, "5.4321");
    private static final ExchangeRate USD_EUR = rate(USD, EUR, "0.89166");
    private static final ExchangeRate EUR_USD = rate(EUR, USD, "1.12151");

    private FakeProvider provider;
    private FakeRepository repository;
    private FakeCache cache;
    private ExchangeRateRefreshService service;

    @BeforeEach
    void setUp() {
        provider = new FakeProvider();
        provider.ratesByBase.put(USD, List.of(USD_BRL, USD_EUR));
        provider.ratesByBase.put(EUR, List.of(EUR_USD));
        repository = new FakeRepository();
        cache = new FakeCache();
        service = new ExchangeRateRefreshService(provider, repository, cache);
    }

    @Test
    @DisplayName("deve buscar, gravar o histórico e popular o cache de todas as moedas base")
    void shouldRefreshAllBases() {
        RefreshReport report = service.refresh(List.of(USD, EUR));

        assertThat(report.requestedBases()).isEqualTo(2);
        assertThat(report.failedBases()).isEmpty();
        assertThat(report.ratesFetched()).isEqualTo(3);
        assertThat(report.ratesInserted()).isEqualTo(3);
        assertThat(repository.stored).containsExactlyInAnyOrder(USD_BRL, USD_EUR, EUR_USD);
        assertThat(cache.find(USD, BRL)).contains(USD_BRL);
        assertThat(cache.find(USD, EUR)).contains(USD_EUR);
        assertThat(cache.find(EUR, USD)).contains(EUR_USD);
    }

    @Test
    @DisplayName("deve usar a gravação em lote do cache uma vez por moeda base")
    void shouldUseBulkCacheWrite() {
        service.refresh(List.of(USD, EUR));

        assertThat(cache.putAllCalls).isEqualTo(2);
    }

    @Test
    @DisplayName("não deve contar como inseridas as taxas já existentes em uma nova atualização")
    void shouldNotCountExistingRatesAsInserted() {
        service.refresh(List.of(USD, EUR));

        RefreshReport second = service.refresh(List.of(USD, EUR));

        assertThat(second.ratesFetched()).isEqualTo(3);
        assertThat(second.ratesInserted()).isZero();
        assertThat(repository.stored).hasSize(3);
    }

    @Test
    @DisplayName("deve continuar as demais moedas quando o provedor falha em uma delas")
    void shouldContinueAfterProviderFailure() {
        provider.failures.put(EUR, new ExchangeRateProviderException("fora do ar"));

        RefreshReport report = service.refresh(List.of(EUR, USD));

        assertThat(report.failedBases()).containsExactly(EUR);
        assertThat(report.isTotalFailure()).isFalse();
        assertThat(report.ratesFetched()).isEqualTo(2);
        assertThat(repository.stored).containsExactlyInAnyOrder(USD_BRL, USD_EUR);
        assertThat(cache.find(EUR, USD)).isEmpty();
    }

    @Test
    @DisplayName("deve tratar moeda base não encontrada como falha da moeda, sem interromper")
    void shouldTreatNotFoundAsBaseFailure() {
        provider.failures.put(EUR, new ExchangeRateNotFoundException("não encontrada"));

        RefreshReport report = service.refresh(List.of(EUR, USD));

        assertThat(report.failedBases()).containsExactly(EUR);
        assertThat(report.ratesFetched()).isEqualTo(2);
    }

    @Test
    @DisplayName("deve reportar falha total quando todas as moedas falham")
    void shouldReportTotalFailure() {
        provider.failures.put(USD, new ExchangeRateProviderException("fora do ar"));
        provider.failures.put(EUR, new ExchangeRateProviderException("fora do ar"));

        RefreshReport report = service.refresh(List.of(USD, EUR));

        assertThat(report.isTotalFailure()).isTrue();
        assertThat(report.ratesFetched()).isZero();
        assertThat(repository.stored).isEmpty();
    }

    @Test
    @DisplayName("deve consultar o provedor uma única vez por moeda base repetida")
    void shouldDeduplicateBases() {
        RefreshReport report = service.refresh(List.of(USD, USD, EUR));

        assertThat(report.requestedBases()).isEqualTo(2);
        assertThat(provider.requested).containsExactly(USD, EUR);
    }

    @Test
    @DisplayName("deve retornar relatório vazio sem chamar o provedor quando não há moedas")
    void shouldHandleEmptyBases() {
        RefreshReport report = service.refresh(List.of());

        assertThat(report.requestedBases()).isZero();
        assertThat(report.isSuccess()).isTrue();
        assertThat(report.isTotalFailure()).isFalse();
        assertThat(provider.requested).isEmpty();
    }

    @Test
    @DisplayName("deve propagar falhas inesperadas como a do banco de dados")
    void shouldPropagateUnexpectedFailures() {
        repository.failure = new IllegalStateException("banco indisponível");

        assertThatThrownBy(() -> service.refresh(List.of(USD)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("deve rejeitar lista de moedas nula")
    void shouldRejectNullBases() {
        assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(NullPointerException.class);
    }

    private static ExchangeRate rate(CurrencyCode base, CurrencyCode quote, String value) {
        return new ExchangeRate(base, quote, new BigDecimal(value), QUOTED_AT);
    }

    private static final class FakeProvider implements ExchangeRateProvider {

        private final Map<CurrencyCode, List<ExchangeRate>> ratesByBase = new HashMap<>();
        private final Map<CurrencyCode, RuntimeException> failures = new HashMap<>();
        private final List<CurrencyCode> requested = new ArrayList<>();

        @Override
        public ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ExchangeRate> fetchRates(CurrencyCode base) {
            requested.add(base);
            RuntimeException failure = failures.get(base);
            if (failure != null) {
                throw failure;
            }
            return ratesByBase.getOrDefault(base, List.of());
        }
    }

    private static final class FakeRepository implements ExchangeRateRepository {

        private final List<ExchangeRate> stored = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public int saveAll(Collection<ExchangeRate> rates) {
            if (failure != null) {
                throw failure;
            }
            int inserted = 0;
            for (ExchangeRate rate : rates) {
                if (!stored.contains(rate)) {
                    stored.add(rate);
                    inserted++;
                }
            }
            return inserted;
        }

        @Override
        public Optional<ExchangeRate> findLatest(CurrencyCode base, CurrencyCode quote) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ExchangeRate> findHistory(CurrencyCode base, CurrencyCode quote, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeCache implements ExchangeRateCache {

        private final Map<String, ExchangeRate> store = new HashMap<>();
        private int putAllCalls;

        @Override
        public Optional<ExchangeRate> find(CurrencyCode base, CurrencyCode quote) {
            return Optional.ofNullable(store.get(key(base, quote)));
        }

        @Override
        public void put(ExchangeRate rate) {
            store.put(key(rate.base(), rate.quote()), rate);
        }

        @Override
        public void putAll(Collection<ExchangeRate> rates) {
            putAllCalls++;
            ExchangeRateCache.super.putAll(rates);
        }

        private static String key(CurrencyCode base, CurrencyCode quote) {
            return base + "/" + quote;
        }
    }
}