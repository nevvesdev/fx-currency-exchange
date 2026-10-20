package br.com.nevvesdev.fxcurrencyexchange.adapter.out.persistence;

import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ConversionRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Conversion;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.Money;
import br.com.nevvesdev.fxcurrencyexchange.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ConversionPersistenceAdapterIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant CONVERTED_AT_1 = Instant.parse("2026-10-20T10:00:00Z");
    private static final Instant CONVERTED_AT_2 = Instant.parse("2026-10-20T11:00:00Z");
    private static final Instant CONVERTED_AT_3 = Instant.parse("2026-10-20T12:00:00Z");

    @Autowired
    private ConversionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("deve salvar e reidratar a conversão idêntica à original")
    void shouldSaveAndRehydrateConversion() {
        Conversion original = conversion("100.00", "USD", "BRL", "5.25", CONVERTED_AT_1);

        repository.save(original);
        flushAndClear();

        assertThat(repository.findById(original.id())).contains(original);
    }

    @Test
    @DisplayName("deve reidratar corretamente moedas sem casas decimais")
    void shouldRehydrateZeroFractionCurrencies() {
        Conversion original = conversion("100.00", "USD", "JPY", "150.5", CONVERTED_AT_1);

        repository.save(original);
        flushAndClear();

        assertThat(repository.findById(original.id())).contains(original);
        assertThat(original.result().amount()).isEqualByComparingTo("15050");
    }

    @Test
    @DisplayName("deve retornar vazio para identificador inexistente")
    void shouldReturnEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("deve listar as conversões mais recentes respeitando o limite")
    void shouldListRecentConversionsWithLimit() {
        Conversion second = conversion("20.00", "USD", "BRL", "5.25", CONVERTED_AT_2);
        Conversion third = conversion("30.00", "USD", "BRL", "5.25", CONVERTED_AT_3);
        Conversion first = conversion("10.00", "USD", "BRL", "5.25", CONVERTED_AT_1);
        repository.save(second);
        repository.save(third);
        repository.save(first);
        flushAndClear();

        assertThat(repository.findRecent(2)).containsExactly(third, second);
    }

    @Test
    @DisplayName("deve rejeitar limite não positivo")
    void shouldRejectNonPositiveLimit() {
        assertThatThrownBy(() -> repository.findRecent(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Conversion conversion(
            String amount, String from, String to, String rate, Instant convertedAt) {
        ExchangeRate exchangeRate = new ExchangeRate(
                CurrencyCode.of(from), CurrencyCode.of(to), new BigDecimal(rate), QUOTED_AT);
        return Conversion.perform(Money.of(amount, from), exchangeRate, convertedAt);
    }
}