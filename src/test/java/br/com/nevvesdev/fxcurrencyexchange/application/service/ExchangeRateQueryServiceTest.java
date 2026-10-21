package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.ExchangeRateLookup;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RateSource;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateQueryServiceTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final ExchangeRate FRESH =
            new ExchangeRate(USD, BRL, new BigDecimal("5.4321"), Instant.parse("2026-10-20T00:00:00Z"));
    private static final ExchangeRate STALE =
            new ExchangeRate(USD, BRL, new BigDecimal("5.4"), Instant.parse("2026-10-17T00:00:00Z"));

    private FakeCache cache;
    private FakeProvider provider;
    private FakeRepository repository;
    private ExchangeRateQueryService service;

    @BeforeEach
    void setUp() {
        cache = new FakeCache();
        provider = new FakeProvider();
        repository = new FakeRepository();
        service = new ExchangeRateQueryService(cache, provider, repository);
    }

    @Test
    @DisplayName("deve responder pelo cache sem consultar provedor nem banco")
    void shouldServeFromCache() {
        cache.put(FRESH);

        ExchangeRateLookup lookup = service.getRate(USD, BRL);

        assertThat(lookup.rate()).isEqualTo(FRESH);
        assertThat(lookup.source()).isEqualTo(RateSource.CACHE);
        assertThat(provider.calls).isZero();
        assertThat(repository.stored).isEmpty();
    }

    @Test
    @DisplayName("deve buscar no provedor em caso de miss, gravar histórico e popular o cache")
    void shouldFetchFromProviderOnMiss() {
        ExchangeRateLookup lookup = service.getRate(USD, BRL);

        assertThat(lookup.rate()).isEqualTo(FRESH);
        assertThat(lookup.source()).isEqualTo(RateSource.PROVIDER);
        assertThat(repository.stored).containsExactly(FRESH);
        assertThat(cache.find(USD, BRL)).contains(FRESH);
        assertThat(provider.calls).isEqualTo(1);
    }

    @Test
    @DisplayName("deve responder pelo cache na segunda consulta")
    void shouldHitCacheOnSecondCall() {
        service.getRate(USD, BRL);

        ExchangeRateLookup second = service.getRate(USD, BRL);

        assertThat(second.source()).isEqualTo(RateSource.CACHE);
        assertThat(provider.calls).isEqualTo(1);
    }

    @Test
    @DisplayName("deve usar a última taxa do banco quando o provedor falha, sem cacheá-la")
    void shouldFallbackToDatabaseWhenProviderFails() {
        provider.failure = new ExchangeRateProviderException("fora do ar");
        repository.stored.add(STALE);

        ExchangeRateLookup lookup = service.getRate(USD, BRL);

        assertThat(lookup.rate()).isEqualTo(STALE);
        assertThat(lookup.source()).isEqualTo(RateSource.DATABASE);
        assertThat(cache.find(USD, BRL)).isEmpty();
    }

    @Test
    @DisplayName("deve propagar a falha do provedor quando o banco não tem taxa")
    void shouldRethrowWhenProviderFailsAndDatabaseIsEmpty() {
        provider.failure = new ExchangeRateProviderException("fora do ar");

        assertThatThrownBy(() -> service.getRate(USD, BRL))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    @DisplayName("deve propagar taxa não encontrada sem usar fallback nem gravar nada")
    void shouldPropagateNotFoundWithoutFallback() {
        provider.failure = new ExchangeRateNotFoundException("par inexistente");
        repository.stored.add(STALE);

        assertThatThrownBy(() -> service.getRate(USD, BRL))
                .isInstanceOf(ExchangeRateNotFoundException.class);
        assertThat(cache.find(USD, BRL)).isEmpty();
        assertThat(repository.stored).containsExactly(STALE);
    }

    @Test
    @DisplayName("deve rejeitar consulta com moedas iguais sem chamar o provedor")
    void shouldRejectSameCurrencies() {
        assertThatThrownBy(() -> service.getRate(USD, USD))
                .isInstanceOf(InvalidExchangeRateException.class);
        assertThat(provider.calls).isZero();
    }

    @Test
    @DisplayName("deve rejeitar moedas nulas")
    void shouldRejectNulls() {
        assertThatThrownBy(() -> service.getRate(null, BRL)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.getRate(USD, null)).isInstanceOf(NullPointerException.class);
    }

    private static final class FakeCache implements ExchangeRateCache {

        private final Map<String, ExchangeRate> store = new HashMap<>();

        @Override
        public Optional<ExchangeRate> find(CurrencyCode base, CurrencyCode quote) {
            return Optional.ofNullable(store.get(key(base, quote)));
        }

        @Override
        public void put(ExchangeRate rate) {
            store.put(key(rate.base(), rate.quote()), rate);
        }

        private static String key(CurrencyCode base, CurrencyCode quote) {
            return base + "/" + quote;
        }
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
            return FRESH;
        }

        @Override
        public List<ExchangeRate> fetchRates(CurrencyCode base) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeRepository implements ExchangeRateRepository {

        private final List<ExchangeRate> stored = new ArrayList<>();

        @Override
        public int saveAll(Collection<ExchangeRate> rates) {
            stored.addAll(rates);
            return rates.size();
        }

        @Override
        public Optional<ExchangeRate> findLatest(CurrencyCode base, CurrencyCode quote) {
            return stored.stream()
                    .filter(rate -> rate.base().equals(base) && rate.quote().equals(quote))
                    .max(Comparator.comparing(ExchangeRate::quotedAt));
        }

        @Override
        public List<ExchangeRate> findHistory(CurrencyCode base, CurrencyCode quote, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }
    }
}