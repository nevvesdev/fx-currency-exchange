package br.com.nevvesdev.fxcurrencyexchange.adapter.out.cache;

import br.com.nevvesdev.fxcurrencyexchange.application.port.out.ExchangeRateCache;
import br.com.nevvesdev.fxcurrencyexchange.domain.exception.DomainException;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.CurrencyCode;
import br.com.nevvesdev.fxcurrencyexchange.domain.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class RedisExchangeRateCache implements ExchangeRateCache {

    private static final Logger log = LoggerFactory.getLogger(RedisExchangeRateCache.class);
    private static final String KEY_PREFIX = "fx:rate:v1:";
    private static final String VALUE_SEPARATOR = ";";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisExchangeRateCache(StringRedisTemplate redisTemplate, FxCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.ttl = properties.rateTtl();
    }

    @Override
    public Optional<ExchangeRate> find(CurrencyCode base, CurrencyCode quote) {
        String key = key(base, quote);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? Optional.empty() : parse(base, quote, key, value);
        } catch (DataAccessException e) {
            log.warn("Cache indisponível ao ler a chave {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(ExchangeRate rate) {
        String key = key(rate.base(), rate.quote());
        try {
            redisTemplate.opsForValue().set(key, serialize(rate), ttl);
        } catch (DataAccessException e) {
            log.warn("Cache indisponível ao gravar a chave {}: {}", key, e.getMessage());
        }
    }

    private static String key(CurrencyCode base, CurrencyCode quote) {
        return KEY_PREFIX + base.value() + ":" + quote.value();
    }

    private static String serialize(ExchangeRate rate) {
        return rate.rate().toPlainString() + VALUE_SEPARATOR + rate.quotedAt();
    }

    private static Optional<ExchangeRate> parse(CurrencyCode base, CurrencyCode quote, String key, String value) {
        try {
            String[] parts = value.split(VALUE_SEPARATOR, -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Formato de valor inesperado");
            }
            return Optional.of(new ExchangeRate(base, quote, new BigDecimal(parts[0]), Instant.parse(parts[1])));
        } catch (IllegalArgumentException | DateTimeException | DomainException e) {
            log.warn("Valor inválido no cache para a chave {}, tratando como miss: {}", key, e.getMessage());
            return Optional.empty();
        }
    }
}