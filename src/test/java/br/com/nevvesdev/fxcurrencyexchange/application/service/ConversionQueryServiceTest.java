package br.com.nevvesdev.fxcurrencyexchange.application.service;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ConversionNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ConversionRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionQueryServiceTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final Conversion CONVERSION = Conversion.perform(
            Money.of("100.00", "USD"),
            new ExchangeRate(USD, BRL, new BigDecimal("5.25"), Instant.parse("2026-10-20T00:00:00Z")),
            Instant.parse("2026-10-22T15:30:00Z"));

    private FakeConversionRepository repository;
    private ConversionQueryService service;

    @BeforeEach
    void setUp() {
        repository = new FakeConversionRepository();
        service = new ConversionQueryService(repository);
    }

    @Test
    @DisplayName("deve retornar a conversão encontrada pelo identificador")
    void shouldReturnConversionById() {
        repository.stored = Optional.of(CONVERSION);

        assertThat(service.getById(CONVERSION.id())).isEqualTo(CONVERSION);
    }

    @Test
    @DisplayName("deve lançar exceção quando a conversão não existe")
    void shouldThrowWhenConversionDoesNotExist() {
        assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                .isInstanceOf(ConversionNotFoundException.class);
    }

    @Test
    @DisplayName("deve repassar o limite na listagem recente")
    void shouldPassLimitToRepository() {
        repository.recent = List.of(CONVERSION);

        List<Conversion> result = service.listRecent(5);

        assertThat(result).containsExactly(CONVERSION);
        assertThat(repository.requestedLimit).isEqualTo(5);
    }

    @Test
    @DisplayName("deve rejeitar identificador nulo")
    void shouldRejectNullId() {
        assertThatThrownBy(() -> service.getById(null)).isInstanceOf(NullPointerException.class);
    }

    private static final class FakeConversionRepository implements ConversionRepository {

        private Optional<Conversion> stored = Optional.empty();
        private List<Conversion> recent = List.of();
        private int requestedLimit;

        @Override
        public Conversion save(Conversion conversion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Conversion> findById(UUID id) {
            return stored;
        }

        @Override
        public List<Conversion> findRecent(int limit) {
            requestedLimit = limit;
            return recent;
        }
    }
}