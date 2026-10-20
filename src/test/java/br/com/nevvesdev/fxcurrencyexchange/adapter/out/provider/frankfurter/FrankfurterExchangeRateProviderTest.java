package br.com.nevvesdev.fxcurrencyexchange.adapter.out.provider.frankfurter;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FrankfurterExchangeRateProviderTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");
    private static final CurrencyCode JPY = CurrencyCode.of("JPY");
    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final String SINGLE_RATE_URL = "http://localhost/v1/latest?from=USD&to=BRL";
    private static final String ALL_RATES_URL = "http://localhost/v1/latest?from=USD";

    private MockRestServiceServer server;
    private FrankfurterExchangeRateProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new FrankfurterExchangeRateProvider(builder.build());
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    @DisplayName("deve buscar a taxa de um par de moedas e mapear para o domínio")
    void shouldFetchSingleRate() {
        server.expect(requestTo(SINGLE_RATE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"amount":1.0,"base":"USD","date":"2026-10-20","rates":{"BRL":5.4321}}
                        """, MediaType.APPLICATION_JSON));

        ExchangeRate rate = provider.fetchRate(USD, BRL);

        assertThat(rate).isEqualTo(new ExchangeRate(USD, BRL, new BigDecimal("5.4321"), QUOTED_AT));
    }

    @Test
    @DisplayName("deve buscar todas as taxas de uma moeda base")
    void shouldFetchAllRatesForBase() {
        server.expect(requestTo(ALL_RATES_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"amount":1.0,"base":"USD","date":"2026-10-20","rates":{"BRL":5.4321,"EUR":0.89166,"JPY":150.5}}
                        """, MediaType.APPLICATION_JSON));

        List<ExchangeRate> rates = provider.fetchRates(USD);

        assertThat(rates).hasSize(3);
        assertThat(rates).allMatch(rate -> rate.base().equals(USD));
        assertThat(rates).extracting(ExchangeRate::quote).containsExactlyInAnyOrder(BRL, EUR, JPY);
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 422})
    @DisplayName("deve traduzir 404 e 422 do provedor em taxa não encontrada")
    void shouldTranslateNotFoundStatuses(int status) {
        server.expect(requestTo(SINGLE_RATE_URL))
                .andRespond(withStatus(HttpStatusCode.valueOf(status))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"not found\"}"));

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                .isInstanceOf(ExchangeRateNotFoundException.class);
    }

    @Test
    @DisplayName("deve considerar não encontrada a taxa ausente na resposta do par")
    void shouldReportNotFoundWhenRateMissingInResponse() {
        server.expect(requestTo(SINGLE_RATE_URL))
                .andRespond(withSuccess("""
                        {"amount":1.0,"base":"USD","date":"2026-10-20","rates":{}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                .isInstanceOf(ExchangeRateNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429, 500, 503})
    @DisplayName("deve traduzir demais erros HTTP em falha do provedor")
    void shouldTranslateOtherErrorStatuses(int status) {
        server.expect(requestTo(SINGLE_RATE_URL))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)));

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    @DisplayName("deve traduzir falha de I/O em falha do provedor")
    void shouldTranslateIoFailure() {
        server.expect(requestTo(SINGLE_RATE_URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    @DisplayName("deve traduzir corpo que não é JSON em falha do provedor")
    void shouldTranslateMalformedBody() {
        server.expect(requestTo(SINGLE_RATE_URL))
                .andRespond(withSuccess("isto não é json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    @DisplayName("deve traduzir resposta sem corpo em falha do provedor")
    void shouldTranslateEmptyBody() {
        server.expect(requestTo(SINGLE_RATE_URL))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> provider.fetchRate(USD, BRL))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    @DisplayName("deve rejeitar moedas nulas")
    void shouldRejectNullArguments() {
        assertThatThrownBy(() -> provider.fetchRate(null, BRL)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> provider.fetchRate(USD, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> provider.fetchRates(null)).isInstanceOf(NullPointerException.class);
        server.reset();
    }
}