package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "FX_LIVE_TESTS", matches = "true")
class FrankfurterExchangeRateProviderLiveTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");

    private final FrankfurterExchangeRateProvider provider = new FrankfurterExchangeRateProvider(
            new FrankfurterClientConfig().frankfurterRestClient(
                    new FrankfurterProperties(
                            "https://api.frankfurter.dev/v1",
                            Duration.ofSeconds(3),
                            Duration.ofSeconds(10))));

    @Test
    @DisplayName("deve buscar uma taxa real USD/BRL")
    void shouldFetchRealRate() {
        ExchangeRate rate = provider.fetchRate(USD, BRL);

        assertThat(rate.base()).isEqualTo(USD);
        assertThat(rate.quote()).isEqualTo(BRL);
        assertThat(rate.rate()).isPositive();
        assertThat(rate.quotedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("deve buscar as taxas reais de USD contra várias moedas")
    void shouldFetchRealRatesForBase() {
        List<ExchangeRate> rates = provider.fetchRates(USD);

        assertThat(rates).isNotEmpty();
        assertThat(rates).extracting(ExchangeRate::quote).contains(BRL, EUR);
    }
}