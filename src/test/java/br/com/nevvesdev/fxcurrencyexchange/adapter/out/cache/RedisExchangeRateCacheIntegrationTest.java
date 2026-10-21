package br.com.nevvesdev.fxcurrencyexchange.adapter.out.cache;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RedisExchangeRateCacheIntegrationTest {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(REDIS_PORT);

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");
    private static final Instant QUOTED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final String USD_BRL_KEY = "fx:rate:v1:USD:BRL";

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private RedisExchangeRateCache cache;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void stopClient() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys("fx:rate:v1:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        cache = new RedisExchangeRateCache(redisTemplate, new FxCacheProperties(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("deve gravar e ler a taxa idêntica à original")
    void shouldStoreAndReadRate() {
        ExchangeRate rate = rate(BRL, "5.4321");

        cache.put(rate);

        assertThat(cache.find(USD, BRL)).contains(rate);
    }

    @Test
    @DisplayName("deve retornar vazio quando a chave não existe")
    void shouldReturnEmptyOnMiss() {
        assertThat(cache.find(USD, BRL)).isEmpty();
    }

    @Test
    @DisplayName("deve aplicar o TTL configurado na chave")
    void shouldApplyConfiguredTtl() {
        cache.put(rate(BRL, "5.4321"));

        Long remainingSeconds = redisTemplate.getExpire(USD_BRL_KEY, TimeUnit.SECONDS);

        assertThat(remainingSeconds).isBetween(1L, 600L);
    }

    @Test
    @DisplayName("deve sobrescrever a taxa anterior do mesmo par")
    void shouldOverwritePreviousRate() {
        cache.put(rate(BRL, "5.40"));
        ExchangeRate updated = rate(BRL, "5.55");
        cache.put(updated);

        assertThat(cache.find(USD, BRL)).contains(updated);
    }

    @Test
    @DisplayName("deve manter os pares isolados entre si")
    void shouldKeepPairsIsolated() {
        ExchangeRate usdBrl = rate(BRL, "5.4321");
        ExchangeRate usdEur = rate(EUR, "0.89166");
        cache.put(usdBrl);
        cache.put(usdEur);

        assertThat(cache.find(USD, BRL)).contains(usdBrl);
        assertThat(cache.find(USD, EUR)).contains(usdEur);
        assertThat(cache.find(EUR, USD)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lixo",
            "abc;2026-10-20T00:00:00Z",
            "5.43;nao-e-data",
            "0;2026-10-20T00:00:00Z",
            ";"
    })
    @DisplayName("deve tratar valor corrompido como miss")
    void shouldTreatCorruptedValueAsMiss(String corrupted) {
        redisTemplate.opsForValue().set(USD_BRL_KEY, corrupted);

        assertThat(cache.find(USD, BRL)).isEmpty();
    }

    private static ExchangeRate rate(CurrencyCode quote, String value) {
        return new ExchangeRate(USD, quote, new BigDecimal(value), QUOTED_AT);
    }
}