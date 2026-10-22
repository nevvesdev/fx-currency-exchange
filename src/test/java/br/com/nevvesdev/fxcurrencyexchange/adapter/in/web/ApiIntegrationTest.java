package br.com.nevvesdev.fxcurrencyexchange.adapter.in.web;

import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateNotFoundException;
import br.com.nevvesdev.fxcurrencyexchange.application.exception.ExchangeRateProviderException;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateProvider;
import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateRepository;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import br.com.nevvesdev.fxcurrencyexchange.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String RATES = "/api/v1/rates";
    private static final String CONVERSIONS = "/api/v1/conversions";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private StubExchangeRateProvider provider;

    @Autowired
    private InMemoryExchangeRateCache cache;

    @Autowired
    private ExchangeRateRepository rateRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        provider.reset();
        cache.clear();
    }

    @Test
    @DisplayName("deve consultar a taxa no provedor e depois servi-la pelo cache")
    void shouldGetRateFromProviderThenCache() throws Exception {
        mockMvc.perform(get(RATES + "/USD/BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.quote").value("BRL"))
                .andExpect(jsonPath("$.rate").value("5.25"))
                .andExpect(jsonPath("$.quotedAt").value("2026-10-20T00:00:00Z"))
                .andExpect(jsonPath("$.source").value("PROVIDER"));

        mockMvc.perform(get(RATES + "/USD/BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("CACHE"));
    }

    @Test
    @DisplayName("deve aceitar códigos de moeda em minúsculas")
    void shouldAcceptLowercaseCurrencyCodes() throws Exception {
        mockMvc.perform(get(RATES + "/usd/brl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("USD"));
    }

    @Test
    @DisplayName("deve usar a última taxa do banco quando o provedor cai")
    void shouldFallbackToDatabaseWhenProviderFails() throws Exception {
        mockMvc.perform(get(RATES + "/USD/BRL")).andExpect(status().isOk());
        cache.clear();
        provider.failure = new ExchangeRateProviderException("fora do ar");

        mockMvc.perform(get(RATES + "/USD/BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value("5.25"))
                .andExpect(jsonPath("$.source").value("DATABASE"));
    }

    @Test
    @DisplayName("deve responder 502 quando o provedor cai e não há taxa anterior")
    void shouldReturnBadGatewayWhenProviderFailsWithoutFallback() throws Exception {
        provider.failure = new ExchangeRateProviderException("fora do ar");

        mockMvc.perform(get(RATES + "/USD/BRL"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    @DisplayName("deve responder 404 para par sem taxa no provedor")
    void shouldReturnNotFoundForUnknownPair() throws Exception {
        mockMvc.perform(get(RATES + "/USD/EUR"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("deve responder 400 para código de moeda inválido")
    void shouldReturnBadRequestForInvalidCurrency() throws Exception {
        mockMvc.perform(get(RATES + "/ABC/BRL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    @DisplayName("deve responder 400 para moedas iguais")
    void shouldReturnBadRequestForSameCurrencies() throws Exception {
        mockMvc.perform(get(RATES + "/USD/USD"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deve listar o histórico do mais recente para o mais antigo")
    void shouldReturnHistoryNewestFirst() throws Exception {
        CurrencyCode usd = CurrencyCode.of("USD");
        CurrencyCode brl = CurrencyCode.of("BRL");
        rateRepository.saveAll(List.of(
                new ExchangeRate(usd, brl, new BigDecimal("5.40"), Instant.parse("2026-10-18T00:00:00Z")),
                new ExchangeRate(usd, brl, new BigDecimal("5.45"), Instant.parse("2026-10-19T00:00:00Z")),
                new ExchangeRate(usd, brl, new BigDecimal("5.50"), Instant.parse("2026-10-20T00:00:00Z"))));

        mockMvc.perform(get(RATES + "/USD/BRL/history").param("from", "2026-10-18").param("to", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-18"))
                .andExpect(jsonPath("$.to").value("2026-10-20"))
                .andExpect(jsonPath("$.rates.length()").value(3))
                .andExpect(jsonPath("$.rates[0].quotedAt").value("2026-10-20T00:00:00Z"))
                .andExpect(jsonPath("$.rates[0].rate").value("5.5"))
                .andExpect(jsonPath("$.rates[2].rate").value("5.4"));
    }

    @Test
    @DisplayName("deve responder 400 para intervalo de histórico inválido")
    void shouldReturnBadRequestForInvalidHistoryRange() throws Exception {
        mockMvc.perform(get(RATES + "/USD/BRL/history").param("from", "2026-10-20").param("to", "2026-10-18"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(RATES + "/USD/BRL/history").param("from", "2024-01-01").param("to", "2026-10-20"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(RATES + "/USD/BRL/history").param("from", "banana"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deve converter, devolver 201 com Location e permitir a consulta por id")
    void shouldCreateConversionAndFetchItById() throws Exception {
        MvcResult created = mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"BRL","amount":100.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(CONVERSIONS + "/")))
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("BRL"))
                .andExpect(jsonPath("$.amount").value("100.00"))
                .andExpect(jsonPath("$.result").value("525.00"))
                .andExpect(jsonPath("$.rate").value("5.25"))
                .andExpect(jsonPath("$.rateSource").value("PROVIDER"))
                .andReturn();

        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get(CONVERSIONS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.result").value("525.00"))
                .andExpect(jsonPath("$.rateSource").doesNotExist());
    }

    @Test
    @DisplayName("deve converter valor em moeda de destino sem casas decimais")
    void shouldConvertIntoZeroFractionCurrency() throws Exception {
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"JPY","amount":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("15050"));
    }

    @Test
    @DisplayName("deve aceitar o valor como string numérica")
    void shouldAcceptAmountAsString() throws Exception {
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"BRL","amount":"20.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("105.00"));
    }

    @Test
    @DisplayName("deve responder 400 com a lista de erros de campo quando o corpo é inválido")
    void shouldReturnFieldErrorsForInvalidBody() throws Exception {
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"BRL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("O campo amount é obrigatório"));

        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"BRL","amount":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }

    @Test
    @DisplayName("deve responder 400 para valor com mais casas decimais que a moeda permite")
    void shouldRejectExcessFractionDigits() throws Exception {
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"BRL","amount":10.005}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("casas decimais")));
    }

    @Test
    @DisplayName("deve responder 400 para moeda inválida, moedas iguais e JSON malformado")
    void shouldRejectInvalidConversionRequests() throws Exception {
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ABC","to":"BRL","amount":10}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"USD","amount":10}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isto não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("deve responder 404 ao converter um par sem taxa disponível")
    void shouldReturnNotFoundWhenConvertingUnknownPair() throws Exception {
        mockMvc.perform(post(CONVERSIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"USD","to":"EUR","amount":10}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deve listar as conversões mais recentes respeitando o limite")
    void shouldListRecentConversions() throws Exception {
        mockMvc.perform(post(CONVERSIONS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"USD\",\"to\":\"BRL\",\"amount\":10.00}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post(CONVERSIONS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"USD\",\"to\":\"BRL\",\"amount\":20.00}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get(CONVERSIONS).param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].amount").value("20.00"));
    }

    @Test
    @DisplayName("deve responder 400 para limite fora do intervalo e identificador inválido")
    void shouldRejectInvalidListParameters() throws Exception {
        mockMvc.perform(get(CONVERSIONS).param("limit", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get(CONVERSIONS).param("limit", "101")).andExpect(status().isBadRequest());
        mockMvc.perform(get(CONVERSIONS + "/nao-e-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deve responder 404 para conversão inexistente")
    void shouldReturnNotFoundForUnknownConversion() throws Exception {
        mockMvc.perform(get(CONVERSIONS + "/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("deve publicar a documentação OpenAPI com os endpoints da API")
    void shouldExposeOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/v1/conversions")))
                .andExpect(content().string(containsString("/api/v1/rates/{base}/{quote}")));
    }

    static class StubExchangeRateProvider implements ExchangeRateProvider {

        private static final Instant QUOTED_AT = Instant.parse("2026-10-20T00:00:00Z");

        private final Map<String, BigDecimal> rates = new HashMap<>();
        private RuntimeException failure;

        void reset() {
            failure = null;
            rates.clear();
            rates.put("USD/BRL", new BigDecimal("5.25"));
            rates.put("USD/JPY", new BigDecimal("150.5"));
        }

        @Override
        public ExchangeRate fetchRate(CurrencyCode base, CurrencyCode quote) {
            if (failure != null) {
                throw failure;
            }
            BigDecimal rate = rates.get(base + "/" + quote);
            if (rate == null) {
                throw new ExchangeRateNotFoundException("Taxa não encontrada: " + base + "/" + quote);
            }
            return new ExchangeRate(base, quote, rate, QUOTED_AT);
        }

        @Override
        public List<ExchangeRate> fetchRates(CurrencyCode base) {
            throw new UnsupportedOperationException();
        }
    }

    static class InMemoryExchangeRateCache implements ExchangeRateCache {

        private final Map<String, ExchangeRate> store = new HashMap<>();

        void clear() {
            store.clear();
        }

        @Override
        public Optional<ExchangeRate> find(CurrencyCode base, CurrencyCode quote) {
            return Optional.ofNullable(store.get(base + "/" + quote));
        }

        @Override
        public void put(ExchangeRate rate) {
            store.put(rate.base() + "/" + rate.quote(), rate);
        }
    }

    @TestConfiguration
    static class StubsConfig {

        @Bean
        @Primary
        StubExchangeRateProvider stubExchangeRateProvider() {
            return new StubExchangeRateProvider();
        }

        @Bean
        @Primary
        InMemoryExchangeRateCache inMemoryExchangeRateCache() {
            return new InMemoryExchangeRateCache();
        }
    }
}