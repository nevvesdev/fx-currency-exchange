package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.InvalidHistoryRangeException;
import br.com.nevvesdev.fxcurrencyexchange.application.model.RateHistory;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateHistoryServiceTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T15:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private FakeRepository repository;
    private RateHistoryService service;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        service = new RateHistoryService(repository, CLOCK);
    }

    @Test
    @DisplayName("deve usar os últimos 30 dias quando nenhum intervalo é informado")
    void shouldUseDefaultRange() {
        RateHistory history = service.getHistory(USD, BRL, null, null);

        assertThat(history.to()).isEqualTo(TODAY);
        assertThat(history.from()).isEqualTo(LocalDate.of(2026, 9, 22));
        assertThat(repository.from).isEqualTo(Instant.parse("2026-09-22T00:00:00Z"));
        assertThat(repository.to).isEqualTo(Instant.parse("2026-10-22T00:00:00Z"));
    }

    @Test
    @DisplayName("deve consultar o repositório com o início do dia em UTC nas duas pontas")
    void shouldQueryWithStartOfDayInUtc() {
        service.getHistory(USD, BRL, LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 20));

        assertThat(repository.base).isEqualTo(USD);
        assertThat(repository.quote).isEqualTo(BRL);
        assertThat(repository.from).isEqualTo(Instant.parse("2026-10-18T00:00:00Z"));
        assertThat(repository.to).isEqualTo(Instant.parse("2026-10-20T00:00:00Z"));
    }

    @Test
    @DisplayName("deve usar hoje como data final quando só a inicial é informada")
    void shouldDefaultToWhenOnlyFromIsProvided() {
        RateHistory history = service.getHistory(USD, BRL, LocalDate.of(2026, 10, 1), null);

        assertThat(history.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(history.to()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("deve usar 30 dias antes da data final quando só a final é informada")
    void shouldDefaultFromWhenOnlyToIsProvided() {
        RateHistory history = service.getHistory(USD, BRL, null, LocalDate.of(2026, 10, 10));

        assertThat(history.from()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(history.to()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    @DisplayName("deve aceitar intervalo de um único dia")
    void shouldAcceptSingleDayRange() {
        RateHistory history = service.getHistory(USD, BRL, TODAY, TODAY);

        assertThat(history.from()).isEqualTo(history.to());
    }

    @Test
    @DisplayName("deve aceitar o intervalo máximo de 366 dias")
    void shouldAcceptMaximumRange() {
        RateHistory history = service.getHistory(USD, BRL, LocalDate.of(2025, 10, 21), TODAY);

        assertThat(history.from()).isEqualTo(LocalDate.of(2025, 10, 21));
    }

    @Test
    @DisplayName("deve rejeitar intervalo maior que 366 dias")
    void shouldRejectRangeAboveMaximum() {
        assertThatThrownBy(() -> service.getHistory(USD, BRL, LocalDate.of(2025, 10, 20), TODAY))
                .isInstanceOf(InvalidHistoryRangeException.class);
    }

    @Test
    @DisplayName("deve rejeitar data inicial posterior à final")
    void shouldRejectInvertedRange() {
        assertThatThrownBy(() -> service.getHistory(USD, BRL, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 18)))
                .isInstanceOf(InvalidHistoryRangeException.class);
    }

    @Test
    @DisplayName("deve rejeitar moedas iguais sem consultar o repositório")
    void shouldRejectSameCurrencies() {
        assertThatThrownBy(() -> service.getHistory(USD, USD, null, null))
                .isInstanceOf(InvalidExchangeRateException.class);
        assertThat(repository.calls).isZero();
    }

    @Test
    @DisplayName("deve devolver as taxas retornadas pelo repositório")
    void shouldReturnRepositoryRates() {
        ExchangeRate rate = new ExchangeRate(USD, BRL, new BigDecimal("5.25"), Instant.parse("2026-10-20T00:00:00Z"));
        repository.result = List.of(rate);

        RateHistory history = service.getHistory(USD, BRL, null, null);

        assertThat(history.rates()).containsExactly(rate);
    }

    @Test
    @DisplayName("deve rejeitar moedas nulas")
    void shouldRejectNulls() {
        assertThatThrownBy(() -> service.getHistory(null, BRL, null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.getHistory(USD, null, null, null)).isInstanceOf(NullPointerException.class);
    }

    private static final class FakeRepository implements ExchangeRateRepository {

        private CurrencyCode base;
        private CurrencyCode quote;
        private Instant from;
        private Instant to;
        private List<ExchangeRate> result = List.of();
        private int calls;

        @Override
        public int saveAll(Collection<ExchangeRate> rates) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ExchangeRate> findLatest(CurrencyCode base, CurrencyCode quote) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ExchangeRate> findHistory(CurrencyCode base, CurrencyCode quote, Instant from, Instant to) {
            this.calls++;
            this.base = base;
            this.quote = quote;
            this.from = from;
            this.to = to;
            return result;
        }
    }
}