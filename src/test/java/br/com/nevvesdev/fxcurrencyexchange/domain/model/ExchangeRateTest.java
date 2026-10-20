package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.CurrencyMismatchException;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidExchangeRateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final CurrencyCode JPY = CurrencyCode.of("JPY");
    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    @DisplayName("deve remover zeros à direita sem usar notação científica")
    void shouldStripTrailingZerosWithoutScientificNotation() {
        ExchangeRate rate = new ExchangeRate(USD, BRL, new BigDecimal("100.00"), QUOTED_AT);

        assertThat(rate.rate().toPlainString()).isEqualTo("100");
    }

    @Test
    @DisplayName("deve considerar iguais taxas equivalentes com escalas diferentes")
    void shouldBeEqualForEquivalentRates() {
        ExchangeRate first = new ExchangeRate(USD, BRL, new BigDecimal("5.2500"), QUOTED_AT);
        ExchangeRate second = new ExchangeRate(USD, BRL, new BigDecimal("5.25"), QUOTED_AT);

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("deve rejeitar moeda base igual à moeda cotada")
    void shouldRejectSameCurrencies() {
        assertThatThrownBy(() -> new ExchangeRate(USD, USD, BigDecimal.ONE, QUOTED_AT))
                .isInstanceOf(InvalidExchangeRateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.000", "-1", "-0.0001"})
    @DisplayName("deve rejeitar taxa zero ou negativa")
    void shouldRejectNonPositiveRate(String rate) {
        assertThatThrownBy(() -> new ExchangeRate(USD, BRL, new BigDecimal(rate), QUOTED_AT))
                .isInstanceOf(InvalidExchangeRateException.class);
    }

    @Test
    @DisplayName("deve rejeitar campos nulos")
    void shouldRejectNulls() {
        assertThatThrownBy(() -> new ExchangeRate(null, BRL, BigDecimal.ONE, QUOTED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ExchangeRate(USD, null, BigDecimal.ONE, QUOTED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ExchangeRate(USD, BRL, null, QUOTED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ExchangeRate(USD, BRL, BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deve converter valor da moeda base para a moeda cotada")
    void shouldConvertFromBaseToQuote() {
        ExchangeRate rate = new ExchangeRate(USD, BRL, new BigDecimal("5.25"), QUOTED_AT);

        Money result = rate.convert(Money.of("100.00", "USD"));

        assertThat(result).isEqualTo(Money.of("525.00", "BRL"));
    }

    @Test
    @DisplayName("deve arredondar o resultado para as casas decimais da moeda cotada")
    void shouldRoundResultToQuoteFractionDigits() {
        ExchangeRate rate = new ExchangeRate(USD, BRL, new BigDecimal("5.2537"), QUOTED_AT);

        Money result = rate.convert(Money.of("10.01", "USD"));

        assertThat(result.amount()).isEqualByComparingTo("52.59");
    }

    @ParameterizedTest
    @CsvSource({"0.5,0", "1.5,2", "2.5,2", "3.5,4"})
    @DisplayName("deve aplicar HALF_EVEN em empates na moeda cotada sem casas decimais")
    void shouldApplyHalfEvenOnTies(String rate, String expected) {
        ExchangeRate exchangeRate = new ExchangeRate(USD, JPY, new BigDecimal(rate), QUOTED_AT);

        Money result = exchangeRate.convert(Money.of("1.00", "USD"));

        assertThat(result.amount()).isEqualByComparingTo(expected);
        assertThat(result.amount().scale()).isZero();
    }

    @Test
    @DisplayName("deve rejeitar conversão de valor em moeda diferente da base")
    void shouldRejectConversionOfWrongCurrency() {
        ExchangeRate rate = new ExchangeRate(USD, BRL, new BigDecimal("5.25"), QUOTED_AT);

        assertThatThrownBy(() -> rate.convert(Money.of("100.00", "BRL")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("deve inverter a taxa trocando base e cotada")
    void shouldInvertRate() {
        ExchangeRate rate = new ExchangeRate(USD, BRL, new BigDecimal("5"), QUOTED_AT);

        ExchangeRate inverse = rate.inverse();

        assertThat(inverse.base()).isEqualTo(BRL);
        assertThat(inverse.quote()).isEqualTo(USD);
        assertThat(inverse.rate()).isEqualByComparingTo("0.2");
        assertThat(inverse.quotedAt()).isEqualTo(QUOTED_AT);
    }

    @Test
    @DisplayName("deve limitar a precisão da inversão de taxas com dízima")
    void shouldLimitPrecisionOnNonTerminatingInversion() {
        ExchangeRate rate = new ExchangeRate(USD, BRL, new BigDecimal("3"), QUOTED_AT);

        assertThat(rate.inverse().rate()).isEqualByComparingTo("0.333333333333333333");
    }
}