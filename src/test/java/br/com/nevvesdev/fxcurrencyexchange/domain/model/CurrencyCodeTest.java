package br.com.nevvesdev.fxcurrencyexchange.domain.model;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidCurrencyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyCodeTest {

    @Test
    @DisplayName("deve normalizar código em minúsculas para maiúsculas")
    void shouldNormalizeLowercaseCode() {
        assertThat(CurrencyCode.of("usd").value()).isEqualTo("USD");
    }

    @Test
    @DisplayName("deve remover espaços nas extremidades do código")
    void shouldTrimSurroundingSpaces() {
        assertThat(CurrencyCode.of("  brl ").value()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("deve considerar iguais códigos equivalentes após normalização")
    void shouldBeEqualAfterNormalization() {
        assertThat(CurrencyCode.of("usd")).isEqualTo(CurrencyCode.of("USD"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("deve rejeitar código nulo, vazio ou em branco")
    void shouldRejectBlankCode(String input) {
        assertThatThrownBy(() -> CurrencyCode.of(input))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "US", "USDX", "12$"})
    @DisplayName("deve rejeitar códigos que não existem na ISO 4217")
    void shouldRejectUnknownCode(String input) {
        assertThatThrownBy(() -> CurrencyCode.of(input))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"XXX", "XAU"})
    @DisplayName("deve rejeitar pseudomoedas sem casas decimais definidas")
    void shouldRejectPseudoCurrencies(String input) {
        assertThatThrownBy(() -> CurrencyCode.of(input))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @ParameterizedTest
    @CsvSource({"USD,2", "BRL,2", "JPY,0", "BHD,3"})
    @DisplayName("deve informar as casas decimais padrão da moeda")
    void shouldExposeFractionDigits(String code, int expectedDigits) {
        assertThat(CurrencyCode.of(code).fractionDigits()).isEqualTo(expectedDigits);
    }
}