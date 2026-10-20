package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrankfurterResponseMapperTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);
    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T00:00:00Z");

    @Test
    @DisplayName("deve mapear cada taxa da resposta para um ExchangeRate do domínio")
    void shouldMapRatesToDomain() {
        FrankfurterResponse response = new FrankfurterResponse(
                "USD", DATE, Map.of("BRL", new BigDecimal("5.4321"), "EUR", new BigDecimal("0.89166")));

        List<ExchangeRate> rates = FrankfurterResponseMapper.toExchangeRates(response);

        assertThat(rates).containsExactlyInAnyOrder(
                new ExchangeRate(USD, BRL, new BigDecimal("5.4321"), QUOTED_AT),
                new ExchangeRate(USD, EUR, new BigDecimal("0.89166"), QUOTED_AT));
    }

    @Test
    @DisplayName("deve retornar lista vazia quando a resposta não traz taxas")
    void shouldReturnEmptyListWhenNoRates() {
        FrankfurterResponse response = new FrankfurterResponse("USD", DATE, Map.of());

        assertThat(FrankfurterResponseMapper.toExchangeRates(response)).isEmpty();
    }

    @Test
    @DisplayName("deve rejeitar resposta nula ou com campos ausentes")
    void shouldRejectIncompleteResponses() {
        Map<String, BigDecimal> rates = Map.of("BRL", new BigDecimal("5.4321"));
        Map<String, BigDecimal> ratesWithNull = new HashMap<>();
        ratesWithNull.put("BRL", null);

        assertThatThrownBy(() -> FrankfurterResponseMapper.toExchangeRates(null))
                .isInstanceOf(ExchangeRateProviderException.class);
        assertThatThrownBy(() -> FrankfurterResponseMapper.toExchangeRates(new FrankfurterResponse(null, DATE, rates)))
                .isInstanceOf(ExchangeRateProviderException.class);
        assertThatThrownBy(() -> FrankfurterResponseMapper.toExchangeRates(new FrankfurterResponse("USD", null, rates)))
                .isInstanceOf(ExchangeRateProviderException.class);
        assertThatThrownBy(() -> FrankfurterResponseMapper.toExchangeRates(new FrankfurterResponse("USD", DATE, null)))
                .isInstanceOf(ExchangeRateProviderException.class);
        assertThatThrownBy(() -> FrankfurterResponseMapper.toExchangeRates(new FrankfurterResponse("USD", DATE, ratesWithNull)))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    @DisplayName("deve traduzir código de moeda inválido do provedor em falha do provedor")
    void shouldTranslateInvalidCurrencyIntoProviderFailure() {
        FrankfurterResponse response = new FrankfurterResponse(
                "USD", DATE, Map.of("ABC", new BigDecimal("1.5")));

        assertThatThrownBy(() -> FrankfurterResponseMapper.toExchangeRates(response))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    @DisplayName("deve traduzir taxa inválida do provedor em falha do provedor")
    void shouldTranslateInvalidRateIntoProviderFailure() {
        FrankfurterResponse response = new FrankfurterResponse(
                "USD", DATE, Map.of("BRL", BigDecimal.ZERO));

        assertThatThrownBy(() -> FrankfurterResponseMapper.toExchangeRates(response))
                .isInstanceOf(ExchangeRateProviderException.class);
    }
}