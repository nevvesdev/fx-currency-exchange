package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");

    @Test
    @DisplayName("deve ajustar a escala para as casas decimais da moeda")
    void shouldScaleToCurrencyFractionDigits() {
        Money money = Money.of("10", "USD");

        assertThat(money.amount().scale()).isEqualTo(2);
        assertThat(money.amount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("deve usar zero casas decimais para moedas como JPY")
    void shouldUseZeroFractionDigitsForJpy() {
        Money money = Money.of("100.4", "JPY");

        assertThat(money.amount().scale()).isZero();
        assertThat(money.amount()).isEqualByComparingTo("100");
    }

    @ParameterizedTest
    @CsvSource({"10.005,10.00", "10.015,10.02", "10.004,10.00", "10.006,10.01"})
    @DisplayName("deve arredondar com HALF_EVEN")
    void shouldRoundHalfEven(String input, String expected) {
        assertThat(Money.of(input, "USD").amount()).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("deve considerar iguais valores equivalentes com escalas diferentes")
    void shouldBeEqualForEquivalentAmounts() {
        assertThat(Money.of("10", "USD")).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    @DisplayName("deve aceitar valor zero")
    void shouldAcceptZero() {
        assertThat(Money.of("0", "USD").isPositive()).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar valor negativo")
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Money.of("-0.01", "USD"))
                .isInstanceOf(InvalidAmountException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "1,50", "NaN"})
    @DisplayName("deve rejeitar texto que não seja um número válido")
    void shouldRejectNonNumericAmount(String input) {
        assertThatThrownBy(() -> Money.of(input, "USD"))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("deve rejeitar valor ou moeda nulos")
    void shouldRejectNulls() {
        assertThatThrownBy(() -> new Money(null, USD)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({"0.01,true", "0.004,false", "0,false", "5,true"})
    @DisplayName("deve indicar se o valor é positivo após o arredondamento")
    void shouldTellIfPositive(String input, boolean expected) {
        assertThat(Money.of(input, "USD").isPositive()).isEqualTo(expected);
    }
}