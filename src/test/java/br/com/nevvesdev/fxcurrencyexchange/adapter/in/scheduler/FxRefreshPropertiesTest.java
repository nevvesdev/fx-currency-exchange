package br.com.nevvesdev.fxcurrencyexchange.adapter.in.scheduler;

import br.com.nevvesdev.fxcurrencyexchange.domain.exception.InvalidCurrencyException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FxRefreshPropertiesTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");

    @Test
    @DisplayName("deve normalizar e remover duplicidades das moedas base")
    void shouldNormalizeAndDeduplicateBases() {
        FxRefreshProperties properties = new FxRefreshProperties(List.of("usd", "EUR", "USD"));

        assertThat(properties.baseCurrencies()).containsExactly(USD, EUR);
    }

    @Test
    @DisplayName("deve rejeitar lista de moedas vazia")
    void shouldRejectEmptyBases() {
        assertThatThrownBy(() -> new FxRefreshProperties(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deve rejeitar lista de moedas nula")
    void shouldRejectNullBases() {
        assertThatThrownBy(() -> new FxRefreshProperties(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deve rejeitar código de moeda inválido na configuração")
    void shouldRejectInvalidCurrency() {
        assertThatThrownBy(() -> new FxRefreshProperties(List.of("USD", "ABC")))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    @DisplayName("deve ligar as moedas a partir da configuração separada por vírgula")
    void shouldBindFromConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfig.class)
                .withPropertyValues("fx.refresh.bases=usd,eur")
                .run(context -> assertThat(context.getBean(FxRefreshProperties.class).baseCurrencies())
                        .containsExactly(USD, EUR));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FxRefreshProperties.class)
    static class PropertiesConfig {
    }
}