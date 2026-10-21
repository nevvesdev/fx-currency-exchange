package br.com.nevvesdev.fxcurrencyexchange.adapter.out.cache;

import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RedisExchangeRateCacheDegradationTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode BRL = CurrencyCode.of("BRL");

    private static LettuceConnectionFactory connectionFactory;
    private static RedisExchangeRateCache cache;

    @BeforeAll
    static void connectToUnreachableRedis() {
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(300))
                .build();
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 1), clientConfiguration);
        connectionFactory.afterPropertiesSet();
        cache = new RedisExchangeRateCache(
                new StringRedisTemplate(connectionFactory), new FxCacheProperties(Duration.ofMinutes(10)));
    }

    @AfterAll
    static void stopClient() {
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("deve tratar leitura como miss quando o Redis está indisponível")
    void shouldTreatReadAsMissWhenRedisIsDown() {
        assertThat(cache.find(USD, BRL)).isEmpty();
    }

    @Test
    @DisplayName("deve ignorar a escrita sem lançar exceção quando o Redis está indisponível")
    void shouldIgnoreWriteWhenRedisIsDown() {
        ExchangeRate rate = new ExchangeRate(
                USD, BRL, new BigDecimal("5.4321"), Instant.parse("2026-10-20T00:00:00Z"));

        assertThatCode(() -> cache.put(rate)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deve ignorar a gravação em lote sem lançar exceção quando o Redis está indisponível")
    void shouldIgnoreBulkWriteWhenRedisIsDown() {
        Instant quotedAt = Instant.parse("2026-10-20T00:00:00Z");
        ExchangeRate usdBrl = new ExchangeRate(USD, BRL, new BigDecimal("5.4321"), quotedAt);
        ExchangeRate usdEur = new ExchangeRate(USD, CurrencyCode.of("EUR"), new BigDecimal("0.89166"), quotedAt);

        assertThatCode(() -> cache.putAll(List.of(usdBrl, usdEur))).doesNotThrowAnyException();
    }
}