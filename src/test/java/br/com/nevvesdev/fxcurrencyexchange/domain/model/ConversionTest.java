package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.CurrencyMismatchException;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T12:00:00Z");
    private static final Instant CONVERTED_AT = Instant.parse("2026-10-20T12:05:00Z");
    private static final ExchangeRate USD_BRL = new ExchangeRate(USD, BRL, new BigDecimal("5.25"), QUOTED_AT);

    @Test
    @DisplayName("deve realizar a conversão aplicando a taxa informada")
    void shouldPerformConversion() {
        Money source = Money.of("100.00", "USD");

        Conversion conversion = Conversion.perform(source, USD_BRL, CONVERTED_AT);

        assertThat(conversion.id()).isNotNull();
        assertThat(conversion.source()).isEqualTo(source);
        assertThat(conversion.result()).isEqualTo(Money.of("525.00", "BRL"));
        assertThat(conversion.rate()).isEqualTo(USD_BRL);
        assertThat(conversion.convertedAt()).isEqualTo(CONVERTED_AT);
    }

    @Test
    @DisplayName("deve gerar identificadores distintos a cada conversão")
    void shouldGenerateDistinctIds() {
        Money source = Money.of("100.00", "USD");

        Conversion first = Conversion.perform(source, USD_BRL, CONVERTED_AT);
        Conversion second = Conversion.perform(source, USD_BRL, CONVERTED_AT);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    @DisplayName("deve rejeitar conversão de valor zero")
    void shouldRejectZeroAmount() {
        Money zero = Money.of("0", "USD");

        assertThatThrownBy(() -> Conversion.perform(zero, USD_BRL, CONVERTED_AT))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("deve rejeitar valor cuja moeda difere da base da taxa")
    void shouldRejectSourceWithWrongCurrency() {
        Money source = Money.of("100.00", "BRL");

        assertThatThrownBy(() -> Conversion.perform(source, USD_BRL, CONVERTED_AT))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("deve rejeitar resultado em moeda diferente da cotada pela taxa")
    void shouldRejectResultWithWrongCurrency() {
        Money source = Money.of("100.00", "USD");
        Money wrongResult = Money.of("525.00", "USD");

        assertThatThrownBy(() -> new Conversion(UUID.randomUUID(), source, wrongResult, USD_BRL, CONVERTED_AT))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("deve rejeitar campos nulos")
    void shouldRejectNulls() {
        Money source = Money.of("100.00", "USD");
        Money result = Money.of("525.00", "BRL");

        assertThatThrownBy(() -> new Conversion(null, source, result, USD_BRL, CONVERTED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Conversion(UUID.randomUUID(), source, result, USD_BRL, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Conversion.perform(source, null, CONVERTED_AT))
                .isInstanceOf(NullPointerException.class);
    }
}